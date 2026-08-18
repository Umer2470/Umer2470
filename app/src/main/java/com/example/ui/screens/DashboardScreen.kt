package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.South
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.UserAccount
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.components.AnalyticsChartsWidget
import com.example.ui.components.ShopLogoAvatar
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.StoreViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Custom Construction Store Color Palette: Navy Blue, White & Gold Accents
val NavyDark = Color(0xFF0F2537)
val NavyLight = Color(0xFF1E3A8A)
val GoldAccent = Color(0xFFD4AF37)
val GoldLight = Color(0xFFFFD700)
val GoldContainer = Color(0xFFFEF3C7)
val CardBorderGold = Color(0xFFF59E0B)

data class ModuleItem(
    val title: String,
    val icon: ImageVector,
    val containerColor: Color,
    val iconColor: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: StoreViewModel,
    onNavigate: (String) -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allSales by viewModel.allSales.collectAsState()
    val allPurchases by viewModel.allPurchases.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val allCustomers by viewModel.allCustomers.collectAsState()
    val allSuppliers by viewModel.allSuppliers.collectAsState()
    val lowStockItems by viewModel.lowStockProducts.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val salesTrend by viewModel.salesTrend7Days.collectAsState()
    val topCategories by viewModel.topCategoriesData.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "RefreshRotation")
    val refreshRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing)
        ),
        label = "Rotation"
    )

    val allStores by viewModel.allStores.collectAsState()
    val selectedStoreId by viewModel.selectedStoreId.collectAsState()
    val activeStore by viewModel.activeStore.collectAsState()
    var showStoreDropdown by remember { mutableStateOf(false) }

    var showLowStockDialog by remember { mutableStateOf(false) }
    var showCashBookModal by remember { mutableStateOf(false) }
    var showQuickAddProductModal by remember { mutableStateOf(false) }
    var showAddExpenseModal by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    var showBarcodeScannerModal by remember { mutableStateOf(false) }
    var showQrScannerModal by remember { mutableStateOf(false) }
    var showBusinessInfoModal by remember { mutableStateOf(false) }
    var showNotificationsModal by remember { mutableStateOf(false) }

    val allUsers by viewModel.allUsers.collectAsState()
    val allActivityLogs by viewModel.allActivityLogs.collectAsState()

    val isAttendanceMinimized by viewModel.isAttendanceMinimized.collectAsState()
    val isAttendanceHidden by viewModel.isAttendanceHidden.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    // Live Date and Current Time state
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTimeString = timeFormat.format(now)
            currentDateString = dateFormat.format(now)
            delay(1000)
        }
    }

    // Calculations for Today's metrics
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfDay = calendar.timeInMillis

    val todaySales = allSales.filter { it.timestamp >= startOfDay }
    val todaySalesTotal = todaySales.sumOf { it.netAmount }

    val todayPurchases = allPurchases.filter { it.timestamp >= startOfDay }
    val todayPurchasesTotal = todayPurchases.sumOf { it.totalAmount }

    val todayExpensesTotal = allExpenses.filter { it.timestamp >= startOfDay }.sumOf { it.amount }
    val todayProfit = (todaySalesTotal - todayPurchasesTotal - todayExpensesTotal).coerceAtLeast(0.0)

    val totalRevenue = allSales.sumOf { it.netAmount }
    val totalExpenses = allExpenses.sumOf { it.amount }
    val cashBalance = totalRevenue - totalExpenses

    val totalCustomerUdhaar = allCustomers.sumOf { it.balance }
    val totalSupplierPayable = allSuppliers.sumOf { it.payableBalance }

    val staffUsers = remember(allUsers) {
        allUsers.filter { it.role != "SUPER_ADMIN" }.ifEmpty { allUsers }
    }
    val totalStaff = staffUsers.size
    val activeStaffCount = if (totalStaff > 0) staffUsers.count { it.isActive } else 1
    val presentCount = staffUsers.count { it.isActive }
    val absentCount = (totalStaff - presentCount).coerceAtLeast(0)

    val timeFormatShort = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val todayLogs = remember(allActivityLogs, startOfDay) {
        allActivityLogs.filter { it.timestamp >= startOfDay }
    }
    val earliestLogToday = todayLogs.minByOrNull { it.timestamp }
    val latestLogToday = todayLogs.maxByOrNull { it.timestamp }

    val checkInTimeText = earliestLogToday?.let { timeFormatShort.format(Date(it.timestamp)) } ?: "09:00 AM"
    val checkOutTimeText = latestLogToday?.let { timeFormatShort.format(Date(it.timestamp)) } ?: "06:00 PM"

    // Quick Search filtered results
    val filteredProducts = if (searchQuery.isNotBlank()) {
        allProducts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.barcode.contains(searchQuery, ignoreCase = true)
        }.take(5)
    } else emptyList()

    val filteredCustomers = if (searchQuery.isNotBlank()) {
        allCustomers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true)
        }.take(3)
    } else emptyList()

    val modules = remember(currentUser) {
        buildList {
            add(ModuleItem("Store Center", Icons.Default.Storefront, Color(0xFFFEF3C7), Color(0xFFD97706)) { onNavigate(Screen.StoreManagement.route) })
            add(ModuleItem("Purchase", Icons.Default.ShoppingBag, Color(0xFFE0F2FE), Color(0xFF0284C7)) { onNavigate(Screen.Purchase.route) })
            add(ModuleItem("Suppliers", Icons.Default.Apartment, Color(0xFFFEF3C7), Color(0xFFD97706)) { onNavigate(Screen.Suppliers.route) })
            add(ModuleItem("Expenses", Icons.Default.AccountBalanceWallet, Color(0xFFFEE2E2), Color(0xFFDC2626)) { showAddExpenseModal = true })
            add(ModuleItem("Cash Book", Icons.Default.MenuBook, Color(0xFFD1FAE5), Color(0xFF059669)) { showCashBookModal = true })
            add(ModuleItem("Invoices & Staff", Icons.Default.ReceiptLong, Color(0xFFF3E8FF), Color(0xFF9333EA)) { onNavigate(Screen.StoreManagement.route) })
            add(ModuleItem("Attendance", Icons.Default.Schedule, Color(0xFFEDE9FE), Color(0xFF6366F1)) { onNavigate(Screen.Attendance.route) })
            add(ModuleItem("Staff Roles", Icons.Default.AdminPanelSettings, Color(0xFFCFFAFE), Color(0xFF0891B2)) { onNavigate(Screen.UserManagement.route) })
            add(ModuleItem("Daily Closing", Icons.Default.ReceiptLong, Color(0xFFFEF3C7), Color(0xFFD97706)) { onNavigate(Screen.DailyClosing.route) })
            add(ModuleItem("Store Access", Icons.Default.Key, Color(0xFFDCFCE7), Color(0xFF16A34A)) { onNavigate(Screen.StoreAccessManagement.route) })
            add(ModuleItem("Activity Logs", Icons.Default.Security, Color(0xFFF1F5F9), Color(0xFF475569)) { onNavigate(Screen.ActivityLogs.route) })
            add(ModuleItem("Security", Icons.Default.Lock, Color(0xFFFFE4E6), Color(0xFFE11D48)) { onNavigate(Screen.StoreAccessManagement.route) })
            add(ModuleItem("Backup & Restore", Icons.Default.Refresh, Color(0xFFE0F2FE), Color(0xFF0284C7)) { onNavigate(Screen.Settings.route) })
            add(ModuleItem("Barcode Scanner", Icons.Default.Search, Color(0xFFEDE9FE), Color(0xFF6366F1)) { showBarcodeScannerModal = true })
            add(ModuleItem("QR Scanner", Icons.Default.Search, Color(0xFFD1FAE5), Color(0xFF059669)) { showQrScannerModal = true })
            add(ModuleItem("Business Info", Icons.Default.Storefront, Color(0xFFFEF3C7), Color(0xFFD97706)) { showBusinessInfoModal = true })
            add(ModuleItem("Notifications", Icons.Default.Warning, Color(0xFFFFE4E6), Color(0xFFE11D48)) { showNotificationsModal = true })
            add(ModuleItem("Settings", Icons.Default.Settings, Color(0xFFF1F5F9), Color(0xFF475569)) { onNavigate(Screen.Settings.route) })
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = showFabMenu, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Surface(
                            onClick = {
                                showFabMenu = false
                                onNavigate(Screen.SalesPos.route)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = NavyDark,
                            tonalElevation = 6.dp,
                            border = BorderStroke(1.dp, GoldAccent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PointOfSale, contentDescription = null, tint = GoldLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("New Sale (POS)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Surface(
                            onClick = {
                                showFabMenu = false
                                showQuickAddProductModal = true
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = NavyDark,
                            tonalElevation = 6.dp,
                            border = BorderStroke(1.dp, GoldAccent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = GoldLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Product", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Surface(
                            onClick = {
                                showFabMenu = false
                                onNavigate(Screen.Purchase.route)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = NavyDark,
                            tonalElevation = 6.dp,
                            border = BorderStroke(1.dp, GoldAccent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = GoldLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("New Purchase", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = GoldAccent,
                    contentColor = NavyDark,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Quick Actions",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshDashboardData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. STORE HEADER CARD (Logo, Urdu Store Name, English Name, Date & Time)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyDark),
                    border = BorderStroke(1.5.dp, GoldAccent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Shop Logo Top Left
                            ShopLogoAvatar(
                                logoUri = settings.logoUri,
                                size = 56.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = settings.storeName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    ),
                                    color = GoldLight
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = settings.ownerName.ifBlank { "Proprietor / Store Management" },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            // Store Switcher Dropdown for Super Admin
                            if (viewModel.isSuperAdmin() && allStores.isNotEmpty()) {
                                Box {
                                    Surface(
                                        onClick = { showStoreDropdown = true },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (selectedStoreId == 0L) "All Stores" else (activeStore?.storeName?.take(10) ?: "Store"),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showStoreDropdown,
                                        onDismissRequest = { showStoreDropdown = false }
                                    ) {
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
                                                showStoreDropdown = false
                                            }
                                        )
                                        HorizontalDivider()
                                        allStores.forEach { store ->
                                            val isSelected = selectedStoreId == store.id
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            store.storeName,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(store.ownerName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.selectStore(store.id)
                                                    showStoreDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Active Store Visual Indicator Banner (Name & Address confirmation)
                        val activeStoreName = if (selectedStoreId == 0L) "Combined View (All Stores)" else (activeStore?.storeName ?: settings.storeName)
                        val activeStoreAddress = if (selectedStoreId == 0L) "Multi-branch Consolidated Inventory & Sales Data" else (activeStore?.address?.ifBlank { settings.address } ?: settings.address)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Active Store Location",
                                        tint = GoldLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "ACTIVE BRANCH: ",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = GoldLight
                                        )
                                        Text(
                                            text = activeStoreName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (activeStoreAddress.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = activeStoreAddress,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Date & Time Display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentDateString.isNotEmpty()) currentDateString else "Loading Date...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = GoldContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (currentTimeString.isNotEmpty()) currentTimeString else "12:00 PM",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = NavyDark,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    onClick = { viewModel.refreshDashboardData() },
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f))
                                ) {
                                    Box(
                                        modifier = Modifier.padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh Dashboard",
                                            tint = GoldLight,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .then(if (isRefreshing) Modifier.rotate(refreshRotationAngle) else Modifier)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. DASHBOARD SEARCH BAR AT TOP
            item {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search products, barcode, customers...", color = Color(0xFF64748B), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldAccent) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        singleLine = true
                    )

                    // Filtered Quick Search Results Box
                    if (searchQuery.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            border = BorderStroke(1.dp, GoldAccent)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "SEARCH RESULTS FOR \"$searchQuery\"",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (filteredProducts.isEmpty() && filteredCustomers.isEmpty()) {
                                    Text("No matching items found", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                                } else {
                                    filteredProducts.forEach { product ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    searchQuery = ""
                                                    onNavigate(Screen.SalesPos.route)
                                                }
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = NavyLight, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("Category: ${product.category} • Stock: ${product.stockQuantity.toInt()} ${product.unit}", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                            Text("${settings.currencySymbol} ${product.salePrice.toInt()}", fontWeight = FontWeight.Bold, color = NavyLight, fontSize = 13.sp)
                                        }
                                        HorizontalDivider(color = Color(0xFFF1F5F9))
                                    }

                                    filteredCustomers.forEach { customer ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    searchQuery = ""
                                                    onNavigate(Screen.Customers.route)
                                                }
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("Phone: ${customer.phone}", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                            Text("Udhaar: ${settings.currencySymbol} ${customer.balance.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. DASHBOARD WIDGETS SECTION HEADER WITH REFRESH BUTTON
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "REAL-TIME DASHBOARD WIDGETS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = NavyDark
                        )
                        Text(
                            text = "Sales, Low Stock & Supplier Payments",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }

                    Surface(
                        onClick = { viewModel.refreshDashboardData() },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.2.dp, GoldAccent),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Figures",
                                tint = GoldAccent,
                                modifier = Modifier
                                    .size(16.dp)
                                    .then(if (isRefreshing) Modifier.rotate(refreshRotationAngle) else Modifier)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isRefreshing) "Refreshing..." else "Refresh Figures",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                        }
                    }
                }
            }

            // 4. CONTROL CENTER METRICS GRID (8 Comprehensive Store Performance Cards)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: Today's Sales & Today's Purchase
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LargeMetricCard(
                            title = "Today's Sales",
                            value = "${settings.currencySymbol} ${todaySalesTotal.toInt()}",
                            subtitle = "${todaySales.size} Sales Made",
                            icon = Icons.Default.MonetizationOn,
                            iconBg = Color(0xFFDCFCE7),
                            iconTint = Color(0xFF16A34A),
                            accentBorder = Color(0xFF16A34A),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Screen.SalesPos.route) }
                        )

                        LargeMetricCard(
                            title = "Today's Purchase",
                            value = "${settings.currencySymbol} ${todayPurchasesTotal.toInt()}",
                            subtitle = "${todayPurchases.size} Purchases",
                            icon = Icons.Default.ShoppingBag,
                            iconBg = Color(0xFFE0F2FE),
                            iconTint = Color(0xFF0284C7),
                            accentBorder = Color(0xFF0284C7),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Screen.Purchase.route) }
                        )
                    }

                    // Row 2: Today's Profit & Cash Balance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LargeMetricCard(
                            title = "Today's Profit",
                            value = "${settings.currencySymbol} ${todayProfit.toInt()}",
                            subtitle = "Estimated Net Profit",
                            icon = Icons.Default.TrendingUp,
                            iconBg = Color(0xFFDCFCE7),
                            iconTint = Color(0xFF16A34A),
                            accentBorder = Color(0xFF16A34A),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Screen.Reports.route) }
                        )

                        LargeMetricCard(
                            title = "Cash Balance",
                            value = "${settings.currencySymbol} ${cashBalance.toInt()}",
                            subtitle = "Net Store Cash",
                            icon = Icons.Default.AccountBalanceWallet,
                            iconBg = Color(0xFFFEF3C7),
                            iconTint = Color(0xFFD97706),
                            accentBorder = GoldAccent,
                            modifier = Modifier.weight(1f),
                            onClick = { showCashBookModal = true }
                        )
                    }

                    // Row 3: Low Stock Alert & Total Customers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LargeMetricCard(
                            title = "Low Stock Alert",
                            value = "${lowStockItems.size}",
                            subtitle = "${lowStockItems.count { it.stockQuantity <= 0.0 }} Out of Stock",
                            icon = Icons.Default.Warning,
                            iconBg = Color(0xFFFFE4E6),
                            iconTint = Color(0xFFE11D48),
                            accentBorder = Color(0xFFE11D48),
                            modifier = Modifier.weight(1f),
                            onClick = { showLowStockDialog = true }
                        )

                        LargeMetricCard(
                            title = "Total Customers",
                            value = "${allCustomers.size}",
                            subtitle = "Udhaar: ${settings.currencySymbol} ${totalCustomerUdhaar.toInt()}",
                            icon = Icons.Default.People,
                            iconBg = Color(0xFFF3E8FF),
                            iconTint = Color(0xFF9333EA),
                            accentBorder = Color(0xFF9333EA),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Screen.Customers.route) }
                        )
                    }

                    // Row 4: Total Suppliers & Employee Attendance Summary
                    if (isAttendanceHidden) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LargeMetricCard(
                                title = "Total Suppliers",
                                value = "${allSuppliers.size}",
                                subtitle = "Payable: ${settings.currencySymbol} ${totalSupplierPayable.toInt()}",
                                icon = Icons.Default.Apartment,
                                iconBg = Color(0xFFE0F2FE),
                                iconTint = Color(0xFF0284C7),
                                accentBorder = Color(0xFF0284C7),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onNavigate(Screen.Suppliers.route) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        ShowAttendanceBanner(
                            onShow = { viewModel.setAttendanceHidden(false) }
                        )
                    } else if (isAttendanceMinimized) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LargeMetricCard(
                                title = "Total Suppliers",
                                value = "${allSuppliers.size}",
                                subtitle = "Payable: ${settings.currencySymbol} ${totalSupplierPayable.toInt()}",
                                icon = Icons.Default.Apartment,
                                iconBg = Color(0xFFE0F2FE),
                                iconTint = Color(0xFF0284C7),
                                accentBorder = Color(0xFF0284C7),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate(Screen.Suppliers.route) }
                            )

                            AttendanceMinimizedMetricCard(
                                activeStaffCount = activeStaffCount,
                                presentCount = presentCount,
                                absentCount = absentCount,
                                modifier = Modifier.weight(1f),
                                onExpand = { viewModel.setAttendanceMinimized(false) },
                                onHide = { viewModel.setAttendanceHidden(true) },
                                onClick = { viewModel.setAttendanceMinimized(false) }
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LargeMetricCard(
                                title = "Total Suppliers",
                                value = "${allSuppliers.size}",
                                subtitle = "Payable: ${settings.currencySymbol} ${totalSupplierPayable.toInt()}",
                                icon = Icons.Default.Apartment,
                                iconBg = Color(0xFFE0F2FE),
                                iconTint = Color(0xFF0284C7),
                                accentBorder = Color(0xFF0284C7),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onNavigate(Screen.Suppliers.route) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        AttendanceExpandedWidgetCard(
                            staffUsers = staffUsers,
                            presentCount = presentCount,
                            absentCount = absentCount,
                            checkInTimeText = checkInTimeText,
                            checkOutTimeText = checkOutTimeText,
                            onMinimize = { viewModel.setAttendanceMinimized(true) },
                            onHide = { viewModel.setAttendanceHidden(true) },
                            onManageClick = { onNavigate(Screen.Attendance.route) }
                        )
                    }
                }
            }

            // 4B. GRAPHICAL SALES TRENDS & ANALYTICS WIDGET (7-Day Daily Sales & Category Distribution)
            item {
                AnalyticsChartsWidget(
                    salesTrend = salesTrend,
                    topCategories = topCategories,
                    currencySymbol = settings.currencySymbol
                )
            }

            // 5. MODULES SECTION HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STORE MODULES",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = NavyDark
                    )
                    Text(
                        text = "17 Available",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            // 5. MODULES GRID
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val chunkedModules = modules.chunked(3)
                    chunkedModules.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (item in rowItems) {
                                PremiumModuleCard(
                                    item = item,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 6. RECENT TRANSACTIONS SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT SALES TRANSACTIONS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = NavyDark
                    )
                    TextButton(onClick = { onNavigate(Screen.Reports.route) }) {
                        Text("View All", color = NavyLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // RECENT SALES CARD LIST
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    if (allSales.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Recent Sales Yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Tap 'Sales POS' to make your first transaction!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val recentSales = allSales.take(5)
                            recentSales.forEach { sale ->
                                val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(sale.timestamp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { viewModel.viewInvoice(sale) }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFDCFCE7)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Receipt,
                                                contentDescription = null,
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = sale.invoiceNumber,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = NavyDark
                                            )
                                            Text(
                                                text = "${sale.customerName} • $timeStr",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${settings.currencySymbol} ${sale.netAmount.toInt()}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = NavyDark
                                        )
                                        Surface(
                                            color = if (sale.paymentType == "Cash") Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = sale.paymentType,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (sale.paymentType == "Cash") Color(0xFF16A34A) else Color(0xFFD97706),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. RECENT SYSTEM ACTIVITY LOGS SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT SYSTEM ACTIVITY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = NavyDark
                    )
                    TextButton(onClick = { onNavigate(Screen.ActivityLogs.route) }) {
                        Text("View All", color = NavyLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    if (allActivityLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No recent activities logged", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val recentLogs = allActivityLogs.take(5)
                            recentLogs.forEach { log ->
                                val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onNavigate(Screen.ActivityLogs.route) }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF1F5F9)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = null,
                                                tint = NavyDark,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NavyDark)
                                            Text("${log.userName} • ${log.details}", fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    Text(timeStr, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showBarcodeScannerModal) {
        AlertDialog(
            onDismissRequest = { showBarcodeScannerModal = false },
            title = { Text("Barcode Scanner", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select module to use barcode scanner:")
                    Button(
                        onClick = {
                            showBarcodeScannerModal = false
                            onNavigate(Screen.SalesPos.route)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open POS Sale Barcode Scanner")
                    }
                    OutlinedButton(
                        onClick = {
                            showBarcodeScannerModal = false
                            onNavigate(Screen.Inventory.route)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Inventory Product Lookup")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBarcodeScannerModal = false }) { Text("Close") }
            }
        )
    }

    if (showQrScannerModal) {
        AlertDialog(
            onDismissRequest = { showQrScannerModal = false },
            title = { Text("QR Code Scanner", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Scan QR Code to join store branch or authenticate access:")
                    Button(
                        onClick = {
                            showQrScannerModal = false
                            onNavigate(Screen.StoreAccessManagement.route)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Store Access Portal")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrScannerModal = false }) { Text("Close") }
            }
        )
    }

    if (showBusinessInfoModal) {
        AlertDialog(
            onDismissRequest = { showBusinessInfoModal = false },
            title = { Text("Business Information", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Store Name: ${settings.storeName}", fontWeight = FontWeight.Bold)
                    Text("Owner / Manager: ${settings.ownerName.ifBlank { "Store Owner" }}")
                    Text("Phone: ${settings.phone.ifBlank { "N/A" }}")
                    Text("Address: ${settings.address.ifBlank { "N/A" }}")
                    Text("Currency: ${settings.currencySymbol}")
                    Text("Tax Rate: ${settings.taxPercentage}%")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showBusinessInfoModal = false
                    onNavigate(Screen.Settings.route)
                }) { Text("Edit Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showBusinessInfoModal = false }) { Text("Close") }
            }
        )
    }

    if (showNotificationsModal) {
        AlertDialog(
            onDismissRequest = { showNotificationsModal = false },
            title = { Text("Notifications & Alerts", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (lowStockItems.isNotEmpty()) {
                        Text("⚠️ Low Stock Alert: ${lowStockItems.size} items low in stock!", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                    } else {
                        Text("✅ Inventory Stock levels are healthy.")
                    }
                    if (totalCustomerUdhaar > 0) {
                        Text("ℹ️ Pending Udhaar Balance: ${settings.currencySymbol} ${totalCustomerUdhaar.toInt()}")
                    }
                    Text("⚡ System Status: 100% Offline Mode active.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsModal = false }) { Text("OK") }
            }
        )
    }
}

    // DIALOG 1: Low Stock Alert Breakdown Modal
    if (showLowStockDialog) {
        LowStockBreakdownDialog(
            lowStockProducts = lowStockItems,
            threshold = settings.defaultLowStockThreshold,
            onNavigateToPurchase = { onNavigate(Screen.Purchase.route) },
            onNavigateToInventory = { onNavigate(Screen.Inventory.route) },
            onDismiss = { showLowStockDialog = false }
        )
    }

    // DIALOG 2: Store Cash Book Modal
    if (showCashBookModal) {
        AlertDialog(
            onDismissRequest = { showCashBookModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = NavyDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Store Cash Book Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyDark)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Sales Revenue:", style = MaterialTheme.typography.bodyMedium)
                        Text("${settings.currencySymbol} ${totalRevenue.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Store Expenses:", style = MaterialTheme.typography.bodyMedium)
                        Text("- ${settings.currencySymbol} ${totalExpenses.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    }
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Cash Balance:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${settings.currencySymbol} ${cashBalance.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (cashBalance >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCashBookModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }

    // DIALOG 3: Quick Add Product Modal
    if (showQuickAddProductModal) {
        QuickAddProductDialog(
            viewModel = viewModel,
            onDismiss = { showQuickAddProductModal = false }
        )
    }

    // DIALOG 4: Quick Add Expense Modal
    if (showAddExpenseModal) {
        QuickAddExpenseDialog(
            viewModel = viewModel,
            onDismiss = { showAddExpenseModal = false }
        )
    }
}

@Composable
fun LargeMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    accentBorder: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(118.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.2.dp, accentBorder.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    ),
                    color = NavyDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp
                    ),
                    color = Color(0xFF64748B),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun PremiumModuleCard(
    item: ModuleItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(104.dp)
            .clickable { item.onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(item.containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                ),
                color = NavyDark,
                maxLines = 1
            )
        }
    }
}

@Composable
fun QuickAddProductDialog(
    viewModel: StoreViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General Building Material") }
    var purchasePrice by remember { mutableStateOf("") }
    var salePrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("Pcs") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = GoldAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quick Add New Product", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name (e.g. Cement, Paint, Pipe)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it },
                        label = { Text("Cost Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = salePrice,
                        onValueChange = { salePrice = it },
                        label = { Text("Sale Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("Stock Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (Bag/Pcs/Liter)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pPrice = purchasePrice.toDoubleOrNull() ?: 0.0
                    val sPrice = salePrice.toDoubleOrNull() ?: 0.0
                    val st = stock.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && sPrice > 0) {
                        viewModel.saveProduct(
                            Product(
                                name = name,
                                category = category,
                                purchasePrice = pPrice,
                                salePrice = sPrice,
                                stockQuantity = st,
                                minStockLevel = 5.0,
                                unit = unit,
                                barcode = ""
                            )
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
            ) {
                Text("Save Product", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun QuickAddExpenseDialog(
    viewModel: StoreViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFDC2626))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Record Store Expense", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title (e.g. Shop Rent, Freight)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (Rs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) {
                        viewModel.addExpense(title = title, category = category, amount = amt, note = "")
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
                Text("Save Expense", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LowStockBreakdownDialog(
    lowStockProducts: List<Product>,
    threshold: Double,
    onNavigateToPurchase: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE11D48)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Low Inventory Stock Alert", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                    Text("${lowStockProducts.size} Items below threshold", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (lowStockProducts.isEmpty()) {
                    Text("All inventory items are sufficiently stocked!", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(lowStockProducts) { product ->
                            val isOut = product.stockQuantity <= 0.0
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isOut) Color(0xFFFFE4E6) else Color(0xFFFFFBEB)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                        Text("Category: ${product.category}", fontSize = 11.sp, color = Color.Gray)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            color = if (isOut) Color(0xFFE11D48) else Color(0xFFD97706),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isOut) "OUT OF STOCK" else "LOW: ${product.stockQuantity.toInt()} ${product.unit}",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onNavigateToInventory()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Inventory", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToPurchase()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("Purchase Stock", fontSize = 12.sp)
                }
            }
        }
    )
}

@Composable
fun AttendanceMinimizedMetricCard(
    activeStaffCount: Int,
    presentCount: Int,
    absentCount: Int,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit,
    onHide: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(118.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.2.dp, Color(0xFF6366F1).copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEDE9FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Employee Attendance",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Attendance",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onHide,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Hide Attendance Widget",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onExpand,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand Attendance",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = "$activeStaffCount Staff Active",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    ),
                    color = Color(0xFF0F172A),
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "P: $presentCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text("•", color = Color.Gray, fontSize = 10.sp)
                    Text(
                        text = "A: $absentCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text("•", color = Color.Gray, fontSize = 10.sp)
                    Text(
                        text = "Expand",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6366F1),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceExpandedWidgetCard(
    staffUsers: List<UserAccount>,
    presentCount: Int,
    absentCount: Int,
    checkInTimeText: String,
    checkOutTimeText: String,
    onMinimize: () -> Unit,
    onHide: () -> Unit,
    onManageClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEDE9FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Employee Attendance",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Employee Attendance & Staff Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Live shift tracking & staff roles",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMinimize,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Minimize Attendance",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onHide,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Hide Attendance Widget",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Present", style = MaterialTheme.typography.labelSmall, color = Color(0xFF15803D), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Text("$presentCount Staff", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF14532D), fontSize = 13.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEE2E2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Absent", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Text("$absentCount Staff", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7F1D1D), fontSize = 13.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, Color(0xFF7DD3FC)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Check-In", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0369A1), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Text(checkInTimeText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0C4A6E), fontSize = 12.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Check-Out", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB45309), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Text(checkOutTimeText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF78350F), fontSize = 12.sp)
                    }
                }
            }

            Text(
                text = "STAFF ROLES & ATTENDANCE STATUS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                staffUsers.take(4).forEach { user ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEDE9FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Role: ${user.role}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (user.isActive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = if (user.isActive) "Present" else "Absent",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (user.isActive) Color(0xFF15803D) else Color(0xFFB91C1C),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onManageClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Manage Staff & Full Attendance", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ShowAttendanceBanner(
    onShow: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFEDE9FE).copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShow() }
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
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Show Attendance",
                    tint = Color(0xFF4F46E5),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Employee Attendance Widget Hidden",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3730A3)
                    )
                    Text(
                        text = "Tap to restore Attendance widget to Dashboard",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6366F1)
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF4F46E5)
            ) {
                Text(
                    text = "Show Attendance",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
