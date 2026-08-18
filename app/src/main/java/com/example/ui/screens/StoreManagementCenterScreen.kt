package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.AttendanceRecord
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreProfile
import com.example.data.entity.StoreSettings
import com.example.data.entity.UserAccount
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.StoreViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val StoreNavy = Color(0xFF0F2537)
private val StoreNavyDark = Color(0xFF091724)
private val StoreGold = Color(0xFFFFD700)
private val StoreGoldLight = Color(0xFFFFF3B0)
private val StoreCardBg = Color(0xFF1E293B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreManagementCenterScreen(
    viewModel: StoreViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State collections
    val settings by viewModel.settings.collectAsState()
    val activeStore by viewModel.activeStore.collectAsState()
    val allStores by viewModel.allStores.collectAsState()
    val selectedStoreId by viewModel.selectedStoreId.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val allSales by viewModel.allSales.collectAsState()
    val allPurchases by viewModel.allPurchases.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val allCustomers by viewModel.allCustomers.collectAsState()
    val allSuppliers by viewModel.allSuppliers.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allAttendance by viewModel.allAttendanceRecords.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Store Overview", "Invoices", "Employees", "Attendance & Shifts", "Branch Access")

    // Live Date & Time
    var currentDateString by remember { mutableStateOf("") }
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTimeString = timeFormat.format(now)
            currentDateString = dateFormat.format(now)
            kotlinx.coroutines.delay(1000)
        }
    }

    // Today's metrics calculation from local database
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfDay = calendar.timeInMillis

    val todaySales = remember(allSales, startOfDay) { allSales.filter { it.timestamp >= startOfDay } }
    val todaySalesTotal = remember(todaySales) { todaySales.sumOf { it.netAmount } }

    val totalStockValue = remember(allProducts) { allProducts.sumOf { it.stockQuantity * it.salePrice } }
    val lowStockCount = remember(allProducts) { allProducts.count { it.stockQuantity <= it.minStockLevel } }

    val staffUsers = remember(allUsers) { allUsers.filter { it.role != "SUPER_ADMIN" }.ifEmpty { allUsers } }
    val totalStaff = staffUsers.size
    val activeStaffCount = remember(staffUsers) { staffUsers.count { it.isActive } }

    val totalRevenue = remember(allSales) { allSales.sumOf { it.netAmount } }
    val totalExpenses = remember(allExpenses) { allExpenses.sumOf { it.amount } }
    val cashInHand = totalRevenue - totalExpenses

    val totalCustomerUdhaar = remember(allCustomers) { allCustomers.sumOf { it.balance } }
    val totalSupplierPayables = remember(allSuppliers) { allSuppliers.sumOf { it.payableBalance } }

    Scaffold(
        topBar = {
            Surface(
                color = StoreNavy,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(StoreGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = "Store Management",
                                    tint = StoreGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = settings.storeName.ifBlank { "CH UMER SENTRY STORE" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StoreGoldLight
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "STORE MANAGEMENT CENTER",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${activeStore?.storeName ?: "Main Branch"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }

                        // Date Time Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = StoreGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentTimeString.ifBlank { "Live" },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B132B))
                .padding(innerPadding)
        ) {
            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1C2541),
                contentColor = Color.White,
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) StoreGold else Color.LightGray,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Tab Panels
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                when (selectedTab) {
                    0 -> StoreOverviewTab(
                        settings = settings,
                        todaySalesTotal = todaySalesTotal,
                        todaySalesCount = todaySales.size,
                        totalStockValue = totalStockValue,
                        lowStockCount = lowStockCount,
                        totalStaff = totalStaff,
                        activeStaffCount = activeStaffCount,
                        cashInHand = cashInHand,
                        totalCustomerUdhaar = totalCustomerUdhaar,
                        totalSupplierPayables = totalSupplierPayables,
                        onNavigate = onNavigate,
                        onSelectTab = { selectedTab = it }
                    )
                    1 -> StoreInvoicesTab(
                        allSales = allSales,
                        currencySymbol = settings.currencySymbol,
                        storeName = settings.storeName,
                        viewModel = viewModel
                    )
                    2 -> StoreEmployeesTab(
                        allUsers = allUsers,
                        viewModel = viewModel
                    )
                    3 -> StoreAttendanceTab(
                        allAttendance = allAttendance,
                        allUsers = allUsers,
                        viewModel = viewModel
                    )
                    4 -> StoreBranchAccessTab(
                        allStores = allStores,
                        activeStore = activeStore,
                        selectedStoreId = selectedStoreId,
                        settings = settings,
                        viewModel = viewModel,
                        onNavigate = onNavigate
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 0: STORE OVERVIEW & SUMMARY
// -------------------------------------------------------------------------------------------------
@Composable
fun StoreOverviewTab(
    settings: StoreSettings,
    todaySalesTotal: Double,
    todaySalesCount: Int,
    totalStockValue: Double,
    lowStockCount: Int,
    totalStaff: Int,
    activeStaffCount: Int,
    cashInHand: Double,
    totalCustomerUdhaar: Double,
    totalSupplierPayables: Double,
    onNavigate: (String) -> Unit,
    onSelectTab: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "STORE PERFORMANCE & REAL-TIME SUMMARY",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp
                ),
                color = StoreGoldLight
            )
        }

        // Key Metric Cards Grid (Row 1)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StoreSummaryCard(
                    title = "Today's Sales",
                    value = "${settings.currencySymbol} ${todaySalesTotal.toInt()}",
                    subtitle = "$todaySalesCount Invoices Generated",
                    icon = Icons.Default.PointOfSale,
                    iconBg = Color(0xFF064E3B),
                    iconTint = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTab(1) }
                )

                StoreSummaryCard(
                    title = "Total Stock Value",
                    value = "${settings.currencySymbol} ${totalStockValue.toInt()}",
                    subtitle = "Inventory Valuation",
                    icon = Icons.Default.Inventory2,
                    iconBg = Color(0xFF0C4A6E),
                    iconTint = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.Inventory.route) }
                )
            }
        }

        // Key Metric Cards Grid (Row 2)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StoreSummaryCard(
                    title = "Staff Attendance",
                    value = "$activeStaffCount / $totalStaff Present",
                    subtitle = "Store Team On Duty",
                    icon = Icons.Default.People,
                    iconBg = Color(0xFF4C1D95),
                    iconTint = Color(0xFFC084FC),
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTab(3) }
                )

                StoreSummaryCard(
                    title = "Cash in Hand",
                    value = "${settings.currencySymbol} ${cashInHand.toInt()}",
                    subtitle = "Store Net Cash Register",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconBg = Color(0xFF78350F),
                    iconTint = Color(0xFFFBBF24),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.Reports.route) }
                )
            }
        }

        // Key Metric Cards Grid (Row 3)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StoreSummaryCard(
                    title = "Customer Udhaar",
                    value = "${settings.currencySymbol} ${totalCustomerUdhaar.toInt()}",
                    subtitle = "Total Credit Receivables",
                    icon = Icons.Default.Group,
                    iconBg = Color(0xFF831843),
                    iconTint = Color(0xFFF472B6),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.Customers.route) }
                )

                StoreSummaryCard(
                    title = "Supplier Payables",
                    value = "${settings.currencySymbol} ${totalSupplierPayables.toInt()}",
                    subtitle = "Pending Invoices",
                    icon = Icons.Default.Business,
                    iconBg = Color(0xFF1E293B),
                    iconTint = Color(0xFF94A3B8),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.Suppliers.route) }
                )
            }
        }

        // Quick Operations Hub
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StoreCardBg),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STORE OPERATIONS HUB",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StoreGoldLight
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(Screen.SalesPos.route) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Sale", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onNavigate(Screen.Purchase.route) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Purchase", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSelectTab(1) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp), tint = StoreGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("All Invoices", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onSelectTab(2) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp), tint = StoreGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Staff & Wages", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoreSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StoreCardBg),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = StoreGoldLight
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Color.LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 1: STORE INVOICES CENTER
// -------------------------------------------------------------------------------------------------
@Composable
fun StoreInvoicesTab(
    allSales: List<Sale>,
    currencySymbol: String,
    storeName: String,
    viewModel: StoreViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") }
    var selectedSaleForDetails by remember { mutableStateOf<Sale?>(null) }
    var invoiceItems by remember { mutableStateOf<List<SaleItem>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }

    val filteredSales = remember(allSales, searchQuery, filterType) {
        allSales.filter { sale ->
            val matchesSearch = searchQuery.isBlank() ||
                sale.id.toString().contains(searchQuery, ignoreCase = true) ||
                sale.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                sale.customerName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (filterType) {
                "PAID" -> sale.dueAmount <= 0.0
                "UDHAAR" -> sale.dueAmount > 0.0
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by Invoice # or Customer Name...", color = Color.Gray, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = StoreGold) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StoreCardBg,
                unfocusedContainerColor = StoreCardBg,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = StoreGold,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL" to "All Invoices", "PAID" to "Fully Paid", "UDHAAR" to "Credit / Udhaar").forEach { (type, label) ->
                FilterChip(
                    selected = filterType == type,
                    onClick = { filterType = type },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (filterType == type) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StoreGold,
                        selectedLabelColor = StoreNavyDark,
                        containerColor = StoreCardBg,
                        labelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Summary Bar
        Surface(
            color = StoreCardBg,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredSales.size} Invoices Found",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Total: $currencySymbol ${filteredSales.sumOf { it.netAmount }.toInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = StoreGoldLight
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Invoices List
        if (filteredSales.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Invoices Found", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSales, key = { it.id }) { sale ->
                    InvoiceRowCard(
                        sale = sale,
                        currencySymbol = currencySymbol,
                        dateFormat = dateFormat,
                        onViewDetails = {
                            scope.launch {
                                invoiceItems = viewModel.getSaleItems(sale.id)
                                selectedSaleForDetails = sale
                            }
                        }
                    )
                }
            }
        }
    }

    // Invoice Details Dialog
    if (selectedSaleForDetails != null) {
        val currentSale = selectedSaleForDetails!!
        AlertDialog(
            onDismissRequest = { selectedSaleForDetails = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Invoice #${currentSale.invoiceNumber.ifBlank { currentSale.id.toString() }}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                if (currentSale.dueAmount <= 0.0) "PAID" else "CREDIT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (currentSale.dueAmount <= 0.0) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                            labelColor = Color.White
                        )
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Customer: ${currentSale.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Date: ${dateFormat.format(Date(currentSale.timestamp))}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Itemized Breakdown:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    invoiceItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.productName} (x${item.quantity.toInt()})", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text("$currencySymbol ${item.totalPrice.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal:", style = MaterialTheme.typography.bodySmall)
                        Text("$currencySymbol ${currentSale.totalAmount.toInt()}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (currentSale.discount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981))
                            Text("- $currencySymbol ${currentSale.discount.toInt()}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Total:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("$currencySymbol ${currentSale.netAmount.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF0284C7))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Received Amount:", style = MaterialTheme.typography.bodySmall)
                        Text("$currencySymbol ${currentSale.paidAmount.toInt()}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (currentSale.dueAmount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Udhaar Balance:", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 12.sp)
                            Text("$currencySymbol ${currentSale.dueAmount.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareText = "--- $storeName ---\nInvoice #${currentSale.invoiceNumber}\nCustomer: ${currentSale.customerName}\nTotal: $currencySymbol ${currentSale.netAmount.toInt()}\nPaid: $currencySymbol ${currentSale.paidAmount.toInt()}\nBalance: $currencySymbol ${currentSale.dueAmount.toInt()}\nDate: ${dateFormat.format(Date(currentSale.timestamp))}"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Invoice Details"))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSaleForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun InvoiceRowCard(
    sale: Sale,
    currencySymbol: String,
    dateFormat: SimpleDateFormat,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onViewDetails() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StoreCardBg),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Invoice #${sale.invoiceNumber.ifBlank { sale.id.toString() }}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = StoreGoldLight
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (sale.dueAmount <= 0.0) Color(0xFF064E3B) else Color(0xFF7F1D1D)
                    ) {
                        Text(
                            text = if (sale.dueAmount <= 0.0) "PAID" else "CREDIT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Customer: ${sale.customerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Text(
                    text = dateFormat.format(Date(sale.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currencySymbol ${sale.netAmount.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF38BDF8)
                )
                if (sale.dueAmount > 0) {
                    Text(
                        text = "Udhaar: $currencySymbol ${sale.dueAmount.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF87171),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 2: STORE EMPLOYEES & WAGES
// -------------------------------------------------------------------------------------------------
@Composable
fun StoreEmployeesTab(
    allUsers: List<UserAccount>,
    viewModel: StoreViewModel
) {
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var showEditCashierDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<UserAccount?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val activeCashier by viewModel.activeCashierProfile.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // ACTIVE CASHIER PROFILE CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F394C)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ACTIVE POS CASHIER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                text = activeCashier.name.ifBlank { "Not Assigned" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showEditCashierDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Configure", fontSize = 11.sp)
                    }
                }

                if (activeCashier.designation.isNotBlank() || activeCashier.employeeId.isNotBlank() || activeCashier.phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val metaList = listOfNotNull(
                        if (activeCashier.designation.isNotBlank()) "Role: ${activeCashier.designation}" else null,
                        if (activeCashier.employeeId.isNotBlank()) "ID: ${activeCashier.employeeId}" else null,
                        if (activeCashier.phone.isNotBlank()) "Ph: ${activeCashier.phone}" else null
                    ).joinToString(" • ")
                    Text(
                        text = metaList,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "STORE STAFF & ROLES",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = StoreGoldLight
                )
                Text(
                    text = "${allUsers.size} Staff Members Registered",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Button(
                onClick = { showAddEmployeeDialog = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Staff", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allUsers, key = { it.id }) { user ->
                val isCurrentCashier = activeCashier.name.equals(user.name, ignoreCase = true)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StoreCardBg),
                    border = BorderStroke(1.dp, if (isCurrentCashier) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrentCashier) Color(0xFF0284C7).copy(alpha = 0.3f) else Color(0xFF3B82F6).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCurrentCashier) Icons.Default.Badge else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isCurrentCashier) Color(0xFF38BDF8) else Color(0xFF60A5FA)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(user.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    if (isCurrentCashier) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF0284C7)
                                        ) {
                                            Text(
                                                text = "ACTIVE CASHIER",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (user.role) {
                                            "SUPER_ADMIN" -> Color(0xFF7E22CE)
                                            "ADMIN" -> Color(0xFF1D4ED8)
                                            "CASHIER" -> Color(0xFF047857)
                                            else -> Color(0xFF475569)
                                        }
                                    ) {
                                        Text(
                                            text = user.role,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PIN: ${user.pinCode}", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                }
                                if (user.dailyWageRate > 0 || user.monthlyBaseSalary > 0) {
                                    Text(
                                        text = if (user.monthlyBaseSalary > 0) "Salary: Rs. ${user.monthlyBaseSalary.toInt()}/mo" else "Daily: Rs. ${user.dailyWageRate.toInt()}/day",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StoreGoldLight
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (!isCurrentCashier) {
                                TextButton(
                                    onClick = { viewModel.setCashierFromEmployee(user) },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Set Cashier", fontSize = 11.sp, color = Color(0xFF38BDF8))
                                }
                            }
                            IconButton(onClick = { selectedUserForEdit = user }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Staff", tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }

    // Configure Cashier Profile Dialog
    if (showEditCashierDialog) {
        var editCashierName by remember(settings) { mutableStateOf(settings.defaultCashierName) }
        var editCashierId by remember(settings) { mutableStateOf(settings.defaultCashierEmployeeId) }
        var editCashierPhone by remember(settings) { mutableStateOf(settings.defaultCashierPhone) }
        var editCashierDesignation by remember(settings) { mutableStateOf(settings.defaultCashierDesignation.ifBlank { "Cashier" }) }

        AlertDialog(
            onDismissRequest = { showEditCashierDialog = false },
            title = { Text("Configure Cashier / Operator Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Set the operator name and details that appear on all POS receipts and invoices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = editCashierName,
                        onValueChange = { editCashierName = it },
                        label = { Text("Cashier Name") },
                        placeholder = { Text("e.g. Muhammad Usman") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCashierId,
                        onValueChange = { editCashierId = it },
                        label = { Text("Employee ID (Optional)") },
                        placeholder = { Text("e.g. EMP-001") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCashierDesignation,
                        onValueChange = { editCashierDesignation = it },
                        label = { Text("Designation") },
                        placeholder = { Text("Cashier") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCashierPhone,
                        onValueChange = { editCashierPhone = it },
                        label = { Text("Phone Number (Optional)") },
                        placeholder = { Text("e.g. 0300-1234567") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCashierProfile(
                            name = editCashierName,
                            employeeId = editCashierId,
                            phone = editCashierPhone,
                            designation = editCashierDesignation
                        )
                        showEditCashierDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCashierDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add Staff Dialog
    if (showAddEmployeeDialog) {
        var newName by remember { mutableStateOf("") }
        var newUsername by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var newRole by remember { mutableStateOf("EMPLOYEE") }
        var adminPinVerification by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddEmployeeDialog = false },
            title = { Text("Add Store Staff Member", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newUsername, onValueChange = { newUsername = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("Phone Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newPin, onValueChange = { newPin = it }, label = { Text("4-Digit Access PIN") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("EMPLOYEE", "ADMIN").forEach { role ->
                            FilterChip(
                                selected = newRole == role,
                                onClick = { newRole = role },
                                label = { Text(role, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = adminPinVerification,
                        onValueChange = { adminPinVerification = it },
                        label = { Text("Admin PIN to Confirm") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newPin.isNotBlank()) {
                            val newUser = UserAccount(
                                username = newUsername.ifBlank { newName.lowercase().replace(" ", "_") },
                                name = newName,
                                pinCode = newPin,
                                role = newRole,
                                phone = newPhone
                            )
                            viewModel.saveUser(
                                user = newUser,
                                adminPin = adminPinVerification,
                                onComplete = { success, msg ->
                                    viewModel.showToast(msg)
                                    if (success) showAddEmployeeDialog = false
                                }
                            )
                        } else {
                            Toast.makeText(context, "Please fill Name and PIN", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Add Staff")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEmployeeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Staff Wage Rates Dialog
    if (selectedUserForEdit != null) {
        val user = selectedUserForEdit!!
        var editDailyRate by remember { mutableStateOf(user.dailyWageRate.toString()) }
        var editHourlyRate by remember { mutableStateOf(user.hourlyWageRate.toString()) }
        var editOvertimeRate by remember { mutableStateOf(user.overtimeHourlyRate.toString()) }
        var editMonthlySalary by remember { mutableStateOf(user.monthlyBaseSalary.toString()) }

        AlertDialog(
            onDismissRequest = { selectedUserForEdit = null },
            title = { Text("Edit Wages - ${user.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editDailyRate, onValueChange = { editDailyRate = it }, label = { Text("Daily Wage (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editHourlyRate, onValueChange = { editHourlyRate = it }, label = { Text("Hourly Rate (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editOvertimeRate, onValueChange = { editOvertimeRate = it }, label = { Text("Overtime Hourly (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editMonthlySalary, onValueChange = { editMonthlySalary = it }, label = { Text("Monthly Base Salary (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserWageRates(
                            userId = user.id,
                            dailyWage = editDailyRate.toDoubleOrNull() ?: 0.0,
                            hourlyWage = editHourlyRate.toDoubleOrNull() ?: 0.0,
                            overtimeWage = editOvertimeRate.toDoubleOrNull() ?: 0.0,
                            monthlyBase = editMonthlySalary.toDoubleOrNull() ?: 0.0
                        )
                        selectedUserForEdit = null
                        Toast.makeText(context, "Wages updated successfully", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save Rates")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedUserForEdit = null }) { Text("Cancel") }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 3: STORE ATTENDANCE & SHIFTS
// -------------------------------------------------------------------------------------------------
@Composable
fun StoreAttendanceTab(
    allAttendance: List<AttendanceRecord>,
    allUsers: List<UserAccount>,
    viewModel: StoreViewModel
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val todayDateStr = remember { dateFormat.format(Date()) }

    val todayAttendance = remember(allAttendance, todayDateStr) {
        allAttendance.filter { it.dateStr == todayDateStr }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "TODAY'S STORE SHIFT ATTENDANCE ($todayDateStr)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = StoreGoldLight
        )
        Text(
            text = "Track employee check-in, check-out, and shift hours",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allUsers.filter { it.role != "SUPER_ADMIN" }.ifEmpty { allUsers }, key = { it.id }) { user ->
                val record = todayAttendance.find { it.userId == user.id }
                val isCheckedIn = record?.checkInTime != null && record.checkInTime > 0L
                val isCheckedOut = record?.checkOutTime != null && record.checkOutTime > 0L

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StoreCardBg),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Role: ${user.role}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                            Spacer(modifier = Modifier.height(4.dp))
                            if (isCheckedIn) {
                                val inTime = timeFormat.format(Date(record!!.checkInTime!!))
                                val outTime = if (isCheckedOut) "• Out: ${timeFormat.format(Date(record.checkOutTime!!))}" else "• Active Shift"
                                Text(
                                    text = "In: $inTime $outTime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCheckedOut) Color(0xFF38BDF8) else Color(0xFF10B981),
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text("Not Checked In Today", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }

                        if (!isCheckedIn) {
                            Button(
                                onClick = {
                                    viewModel.checkInEmployee(user.id, user.name, user.role)
                                    Toast.makeText(context, "${user.name} checked in", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Text("Check In", fontSize = 11.sp)
                            }
                        } else if (!isCheckedOut) {
                            Button(
                                onClick = {
                                    viewModel.checkOutEmployee(user.id)
                                    Toast.makeText(context, "${user.name} checked out", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                            ) {
                                Text("Check Out", fontSize = 11.sp)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF064E3B)
                            ) {
                                Text(
                                    text = "COMPLETED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 4: STORE BRANCH ACCESS & SECURITY
// -------------------------------------------------------------------------------------------------
@Composable
fun StoreBranchAccessTab(
    allStores: List<StoreProfile>,
    activeStore: StoreProfile?,
    selectedStoreId: Long,
    settings: StoreSettings,
    viewModel: StoreViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StoreCardBg),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ACTIVE STORE PROFILE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StoreGoldLight
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    StoreInfoRow("Branch Name", activeStore?.storeName ?: settings.storeName)
                    StoreInfoRow("Proprietor", activeStore?.ownerName ?: settings.ownerName)
                    StoreInfoRow("Branch Phone", activeStore?.phone ?: settings.phone)
                    StoreInfoRow("Store Location", activeStore?.address ?: settings.address)
                    StoreInfoRow("Branch Access Code", activeStore?.code ?: "BRANCH-001")

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onNavigate(Screen.StoreAccessManagement.route) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Manage Multi-Branch Security & QR Codes")
                    }
                }
            }
        }

        item {
            Text(
                text = "REGISTERED STORE BRANCHES (${allStores.size})",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp
                ),
                color = StoreGoldLight
            )
        }

        items(allStores, key = { it.id }) { store ->
            val isSelected = selectedStoreId == store.id
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    viewModel.selectStore(store.id)
                },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF1E3A8A) else StoreCardBg),
                border = BorderStroke(1.dp, if (isSelected) StoreGold else Color.White.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(store.storeName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text(store.address.ifBlank { "Main Address" }, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    }

                    if (isSelected) {
                        Surface(shape = RoundedCornerShape(4.dp), color = StoreGold) {
                            Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StoreNavyDark, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    } else {
                        OutlinedButton(onClick = { viewModel.selectStore(store.id) }) {
                            Text("Switch")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

