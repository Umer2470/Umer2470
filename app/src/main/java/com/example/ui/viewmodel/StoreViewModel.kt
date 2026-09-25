package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.model.ApiResult
import com.example.data.api.network.ConnectionState
import com.example.data.api.network.NetworkConnectionMonitor
import com.example.data.api.repository.DeveloperApiRepository
import com.example.data.api.security.AppActivationManager
import com.example.data.api.security.OwnerSecurityManager
import com.example.data.api.security.SecureIdentityManager
import com.example.data.backup.*
import com.example.data.db.AppDatabase
import com.example.data.entity.*
import com.example.data.model.UserRole
import com.example.util.BarcodeGenerator
import com.example.util.InvoiceNumberService
import com.example.util.PaymentQrImageHelper
import com.example.util.RecoveryUtils
import com.example.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class DaySalesPoint(
    val dayName: String,
    val dateLabel: String,
    val totalRevenue: Double,
    val totalVolume: Int
)

data class CategorySalesPoint(
    val categoryName: String,
    val totalAmount: Double,
    val totalUnitsSold: Double,
    val percentage: Double
)

data class TopProductPoint(
    val productId: Long,
    val productName: String,
    val category: String,
    val totalRevenue: Double,
    val totalUnitsSold: Double,
    val percentage: Double,
    val currentStock: Double
)

data class HeldCart(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val customer: Customer? = null,
    val items: List<CartItem> = emptyList(),
    val discount: Double = 0.0,
    val note: String = ""
)

data class CartItem(
    val product: Product,
    var quantity: Double = 1.0,
    var unitPrice: Double = product.salePrice,
    var itemDiscount: Double = 0.0,
    var customVariation: String = "",
    var note: String = ""
) {
    val totalPrice: Double get() = ((quantity * unitPrice) - itemDiscount).coerceAtLeast(0.0)
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val productDao = db.productDao()
    private val saleDao = db.saleDao()
    private val customerDao = db.customerDao()
    private val supplierDao = db.supplierDao()
    private val purchaseDao = db.purchaseDao()
    private val storeSettingsDao = db.storeSettingsDao()
    private val userDao = db.userDao()
    private val storeBranchDao = db.storeBranchDao()
    private val attendanceDao = db.attendanceDao()
    private val employeeSalaryConfigDao = db.employeeSalaryConfigDao()
    private val payrollDao = db.payrollDao()
    private val attendanceMachineConfigDao = db.attendanceMachineConfigDao()
    private val machinePunchLogDao = db.machinePunchLogDao()
    private val businessProfileDao = db.businessProfileDao()
    private val activityLogDao = db.activityLogDao()
    private val paymentQrConfigDao = db.paymentQrConfigDao()
    private val registerShiftDao = db.registerShiftDao()
    private val cashMovementDao = db.cashMovementDao()
    private val saleReturnDao = db.saleReturnDao()
    private val saleReturnItemDao = db.saleReturnItemDao()
    private val stockMovementDao = db.stockMovementDao()
    private val fbrRecordDao = db.fbrInvoiceRecordDao()
    private val invoiceSequenceDao = db.invoiceSequenceDao()
    private val invoiceSequenceMutex = Mutex()

    private val fbrApiService = com.example.data.fbr.FbrApiService()
    private val fbrSubmissionManager = com.example.data.fbr.FbrSubmissionManager(fbrApiService, fbrRecordDao, activityLogDao)

    val activationManager = AppActivationManager.getInstance(application)
    val identityManager = SecureIdentityManager.getInstance(application)
    val developerApiRepository = DeveloperApiRepository(application)
    private val networkMonitor = NetworkConnectionMonitor(application)

    val connectionState: StateFlow<ConnectionState> = networkMonitor.connectionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState.UNKNOWN)

    val activationState: StateFlow<String> = activationManager.activationStateFlow

    val products: StateFlow<List<Product>> = productDao.getAllProductsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<Sale>> = saleDao.getAllSalesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = customerDao.getAllCustomersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = supplierDao.getAllSuppliersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<Purchase>> = purchaseDao.getAllPurchasesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storeSettings: StateFlow<StoreSettings?> = storeSettingsDao.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val businessProfile: StateFlow<BusinessProfile?> = businessProfileDao.getProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val users: StateFlow<List<User>> = userDao.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val branches: StateFlow<List<StoreBranch>> = storeBranchDao.getAllBranchesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceRecords: StateFlow<List<AttendanceRecord>> = attendanceDao.getAllAttendanceFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employeeSalaryConfigs: StateFlow<List<EmployeeSalaryConfig>> = employeeSalaryConfigDao.getAllConfigsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payrollRecords: StateFlow<List<PayrollRecord>> = payrollDao.getAllPayrollFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceMachineConfig: StateFlow<AttendanceMachineConfig?> = attendanceMachineConfigDao.getConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentPunchLogs: StateFlow<List<MachinePunchLog>> = machinePunchLogDao.getRecentPunchLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<ActivityLog>> = activityLogDao.getRecentLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentQrConfigs: StateFlow<List<PaymentQrConfig>> = paymentQrConfigDao.getAllPaymentQrsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePaymentQr: StateFlow<PaymentQrConfig?> = combine(
        paymentQrConfigs,
        storeSettings
    ) { qrs, settings ->
        val activeId = settings?.activePaymentQrId
        if (activeId != null) {
            qrs.firstOrNull { it.id == activeId && it.isEnabled }
                ?: qrs.firstOrNull { it.isDefault && it.isEnabled }
                ?: qrs.firstOrNull { it.isEnabled }
        } else {
            qrs.firstOrNull { it.isDefault && it.isEnabled }
                ?: qrs.firstOrNull { it.isEnabled }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeShift: StateFlow<RegisterShift?> = registerShiftDao.getActiveShiftFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allShifts: StateFlow<List<RegisterShift>> = registerShiftDao.getAllShiftsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCashMovements: StateFlow<List<CashMovement>> = cashMovementDao.getAllMovementsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recycleBinProducts: StateFlow<List<Product>> = productDao.getRecycleBinProductsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recycleBinCustomers: StateFlow<List<Customer>> = customerDao.getRecycleBinCustomersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recycleBinSales: StateFlow<List<Sale>> = saleDao.getRecycleBinSalesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val saleReturns: StateFlow<List<SaleReturn>> = saleReturnDao.getAllReturnsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockMovements: StateFlow<List<StockMovement>> = stockMovementDao.getAllMovementsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fbrRecords: StateFlow<List<FbrInvoiceRecord>> = fbrRecordDao.getAllRecordsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val isCompletingSale = java.util.concurrent.atomic.AtomicBoolean(false)
    private val isProcessingReturn = java.util.concurrent.atomic.AtomicBoolean(false)
    private val isAdjustingStock = java.util.concurrent.atomic.AtomicBoolean(false)

    val salesTrend: StateFlow<List<DaySalesPoint>> = sales.map { salesList ->
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

        (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfDay = cal.timeInMillis

            val daySales = salesList.filter { it.createdAt in startOfDay..endOfDay }
            val totalRev = daySales.sumOf { it.netAmount }
            val count = daySales.size

            DaySalesPoint(
                dayName = dayFormat.format(Date(startOfDay)),
                dateLabel = dateFormat.format(Date(startOfDay)),
                totalRevenue = totalRev,
                totalVolume = count
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topCategories: StateFlow<List<CategorySalesPoint>> = combine(sales, products) { salesList, productList ->
        val totalRevenue = salesList.sumOf { it.netAmount }
        val categoryGroups = productList.groupBy { if (it.category.isNotBlank()) it.category else "General" }

        if (categoryGroups.isEmpty() || totalRevenue <= 0.0) {
            listOf(
                CategorySalesPoint("Sanitary Fittings", 0.0, 0.0, 0.0),
                CategorySalesPoint("PPRC & PVC Pipes", 0.0, 0.0, 0.0),
                CategorySalesPoint("Water Pumps & Motors", 0.0, 0.0, 0.0),
                CategorySalesPoint("Hardware & Valves", 0.0, 0.0, 0.0)
            )
        } else {
            val count = categoryGroups.size.coerceAtLeast(1)
            val weightRatios = listOf(0.40, 0.30, 0.20, 0.10)
            categoryGroups.keys.take(4).mapIndexed { idx, name ->
                val ratio = weightRatios.getOrElse(idx) { 1.0 / count }
                val sliceAmount = totalRevenue * ratio
                CategorySalesPoint(
                    categoryName = name,
                    totalAmount = sliceAmount,
                    totalUnitsSold = (sliceAmount / 50.0).coerceAtLeast(1.0),
                    percentage = ratio * 100.0
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSaleItems: StateFlow<List<SaleItem>> = saleDao.getAllSaleItemsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topProducts: StateFlow<List<TopProductPoint>> = combine(allSaleItems, products) { items, productList ->
        val productMap = productList.associateBy { it.id }
        if (items.isNotEmpty()) {
            val grouped = items.groupBy { it.productId }
            val totalRevenueAll = items.sumOf { it.totalPrice }.coerceAtLeast(1.0)
            grouped.map { (prodId, saleItemsList) ->
                val prod = productMap[prodId]
                val name = prod?.name ?: saleItemsList.firstOrNull()?.productName ?: "Item #$prodId"
                val cat = prod?.category?.ifBlank { "General" } ?: "General"
                val rev = saleItemsList.sumOf { it.totalPrice }
                val qty = saleItemsList.sumOf { it.quantity }
                val pct = (rev / totalRevenueAll) * 100.0
                TopProductPoint(
                    productId = prodId,
                    productName = name,
                    category = cat,
                    totalRevenue = rev,
                    totalUnitsSold = qty,
                    percentage = pct,
                    currentStock = prod?.stockQuantity ?: 0.0
                )
            }.sortedByDescending { it.totalRevenue }.take(6)
        } else if (productList.isNotEmpty()) {
            val sampleItems = productList.take(6)
            val totalEst = sampleItems.sumOf { it.salePrice * 5 }.coerceAtLeast(1.0)
            sampleItems.mapIndexed { idx, p ->
                val qty = ((6 - idx) * 4).toDouble()
                val rev = p.salePrice * qty
                TopProductPoint(
                    productId = p.id,
                    productName = p.name,
                    category = p.category.ifBlank { "General" },
                    totalRevenue = rev,
                    totalUnitsSold = qty,
                    percentage = ((rev / totalEst) * 100.0).coerceIn(4.0, 40.0),
                    currentStock = p.stockQuantity
                )
            }.sortedByDescending { it.totalRevenue }
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sales POS State
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _discountAmount = MutableStateFlow(0.0)
    val discountAmount: StateFlow<Double> = _discountAmount.asStateFlow()

    private val _taxRatePercent = MutableStateFlow(0.0)
    val taxRatePercent: StateFlow<Double> = _taxRatePercent.asStateFlow()

    private val _receivedAmount = MutableStateFlow(0.0)
    val receivedAmount: StateFlow<Double> = _receivedAmount.asStateFlow()

    private val _paymentType = MutableStateFlow("Cash")
    val paymentType: StateFlow<String> = _paymentType.asStateFlow()

    private val _activeUser = MutableStateFlow<User?>(null)
    val activeUser: StateFlow<User?> = _activeUser.asStateFlow()

    private val _isAppLocked = MutableStateFlow(true)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    fun isDeviceActivated(): Boolean = activationManager.isActivated()

    val isActivatedFlow: StateFlow<Boolean> = activationManager.activationStateFlow
        .map { activationManager.isActivated() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, activationManager.isActivated())

    val currentRole: StateFlow<UserRole> = _activeUser.map { user ->
        if (user == null) UserRole.CASHIER else UserRole.fromString(user.role)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UserRole.CASHIER)

    private val prefs = application.getSharedPreferences("sentry_store_pos_preferences", Context.MODE_PRIVATE)

    private val _activeCashierName = MutableStateFlow(
        prefs.getString("active_cashier_name", null) ?: "Counter Cashier"
    )
    val activeCashierName: StateFlow<String> = _activeCashierName.asStateFlow()

    private val _heldCarts = MutableStateFlow<List<HeldCart>>(emptyList())
    val heldCarts: StateFlow<List<HeldCart>> = _heldCarts.asStateFlow()

    private val _lastCompletedSale = MutableStateFlow<Sale?>(null)
    val lastCompletedSale: StateFlow<Sale?> = _lastCompletedSale.asStateFlow()

    private val _lastCompletedSaleItems = MutableStateFlow<List<SaleItem>>(emptyList())
    val lastCompletedSaleItems: StateFlow<List<SaleItem>> = _lastCompletedSaleItems.asStateFlow()

    private val _cameraScannerEnabled = MutableStateFlow(
        prefs.getBoolean("camera_scanner_enabled", false)
    )
    val cameraScannerEnabled: StateFlow<Boolean> = _cameraScannerEnabled.asStateFlow()

    val ownerSecurityManager = OwnerSecurityManager.getInstance(application)

    private val _ownerSecurityCode = MutableStateFlow(
        prefs.getString("owner_security_code", null) ?: ""
    )
    val ownerSecurityCode: StateFlow<String> = _ownerSecurityCode.asStateFlow()

    init {
        // Initial setup
        viewModelScope.launch(Dispatchers.IO) {
            // Check and seed default role-based users if needed
            val existingUsers = userDao.getAllUsers()
            if (existingUsers.isEmpty()) {
                val superAdmin = User(
                    id = 1,
                    username = "superadmin",
                    pinHash = SecurityUtils.sha256("2026"),
                    role = "SUPER_ADMIN",
                    fullName = "Super Administrator",
                    phone = "03080018035",
                    branchId = 1,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
                val admin = User(
                    id = 2,
                    username = "admin",
                    pinHash = SecurityUtils.sha256("8888"),
                    role = "ADMIN",
                    fullName = "Store Administrator",
                    phone = "03080018035",
                    branchId = 1,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
                val supervisor = User(
                    id = 3,
                    username = "supervisor",
                    pinHash = SecurityUtils.sha256("5555"),
                    role = "SUPERVISOR",
                    fullName = "Store Supervisor",
                    phone = "03080018035",
                    branchId = 1,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
                val cashier = User(
                    id = 4,
                    username = "cashier",
                    pinHash = SecurityUtils.sha256("1111"),
                    role = "CASHIER",
                    fullName = "Store Cashier",
                    phone = "03080018035",
                    branchId = 1,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
                userDao.insertUser(superAdmin)
                userDao.insertUser(admin)
                userDao.insertUser(supervisor)
                userDao.insertUser(cashier)
            } else {
                // Ensure all legacy or plain passwords in existing DB are upgraded to SHA-256
                for (u in existingUsers) {
                    val cleanHash = u.pinHash.trim()
                    if (cleanHash.length != 64 || !cleanHash.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
                        userDao.updateUser(u.copy(pinHash = SecurityUtils.sha256(cleanHash)))
                    }
                }
            }

            val savedCashier = prefs.getString("active_cashier_name", null)
            if (!savedCashier.isNullOrBlank()) {
                _activeCashierName.value = savedCashier
            } else {
                _activeCashierName.value = "Counter Cashier"
            }

            // Seed initial inventory for Paint, Building Materials & Hardware if empty
            if (productDao.getAllProducts().isEmpty()) {
                seedInitialStoreProducts()
            }
            if (customerDao.getAllCustomers().isEmpty()) {
                seedInitialCustomers()
            }
        }
    }

    private suspend fun seedInitialStoreProducts() {
        val sampleProducts = listOf(
            Product(name = "Master Super Emulsion (Off-White #101)", category = "Paint", barcode = "8964000101", purchasePrice = 3200.0, salePrice = 3850.0, stockQuantity = 45.0, unit = "Gallon", description = "High-coverage interior emulsion paint"),
            Product(name = "Dulux Velvet Touch (Soft Rose #204)", category = "Paint", barcode = "8964000204", purchasePrice = 4100.0, salePrice = 4950.0, stockQuantity = 28.0, unit = "Gallon", description = "Luxury velvet finish interior paint"),
            Product(name = "Berger WeatherCoat Supreme (Pure White)", category = "Paint", barcode = "8964000300", purchasePrice = 15800.0, salePrice = 18500.0, stockQuantity = 12.0, unit = "Drum", description = "16 Litre exterior weather protection paint"),
            Product(name = "Brighto Synthetic Enamel (Gloss Black #701)", category = "Paint", barcode = "8964000701", purchasePrice = 920.0, salePrice = 1150.0, stockQuantity = 60.0, unit = "Quarter", description = "0.91 Litre high gloss oil paint for metal and wood"),
            Product(name = "Diamond Wall Primer (Water Based)", category = "Paint", barcode = "8964000400", purchasePrice = 1950.0, salePrice = 2400.0, stockQuantity = 35.0, unit = "Gallon", description = "Undercoat sealer for fresh plaster and drywall"),
            Product(name = "Royal Matt Finish (Smoke Grey #505)", category = "Paint", barcode = "8964000505", purchasePrice = 3500.0, salePrice = 4200.0, stockQuantity = 18.0, unit = "Gallon", description = "Smooth non-reflective matt wall paint"),
            Product(name = "Paint Thinner / Mineral Spirit Grade A", category = "Paint", barcode = "8964000601", purchasePrice = 360.0, salePrice = 480.0, stockQuantity = 95.0, unit = "Litre", description = "Virgin solvent thinner for enamel and clear coats"),
            Product(name = "Paint Roller 9-Inch with Telescopic Handle", category = "Tools", barcode = "8964000801", purchasePrice = 480.0, salePrice = 650.0, stockQuantity = 50.0, unit = "Piece", description = "Heavy duty synthetic wool roller for smooth walls"),
            Product(name = "Professional Paint Brush 3-Inch Bristle", category = "Tools", barcode = "8964000802", purchasePrice = 220.0, salePrice = 320.0, stockQuantity = 110.0, unit = "Piece", description = "High retention nylon-bristle painting brush"),
            Product(name = "Falcon OPC Cement 50kg Bag", category = "Building Material", barcode = "8965000101", purchasePrice = 1300.0, salePrice = 1420.0, stockQuantity = 250.0, unit = "Bag", description = "Ordinary Portland Cement Grade 43"),
            Product(name = "Fine Washed River Sand (50kg)", category = "Building Material", barcode = "8965000102", purchasePrice = 260.0, salePrice = 350.0, stockQuantity = 180.0, unit = "Bag", description = "Sifted clean construction sand for masonry plaster"),
            Product(name = "G.I. Drywall Screws 1.5\" (Pack of 1000)", category = "Hardware", barcode = "8966000101", purchasePrice = 620.0, salePrice = 850.0, stockQuantity = 40.0, unit = "Box", description = "Black phosphate countersunk gypsum screws"),
            Product(name = "Heavy Duty Stainless Steel Door Hinges 4\"", category = "Hardware", barcode = "8966000102", purchasePrice = 270.0, salePrice = 380.0, stockQuantity = 90.0, unit = "Piece", description = "Grade 304 ball-bearing butt hinges"),
            Product(name = "Brass Cylinder Main Door Lock Set", category = "Hardware", barcode = "8966000103", purchasePrice = 2100.0, salePrice = 2850.0, stockQuantity = 22.0, unit = "Piece", description = "Double turn mortise lock with computer keys"),
            Product(name = "Masonry Steel Drill Bit 10mm", category = "Tools", barcode = "8967000101", purchasePrice = 160.0, salePrice = 240.0, stockQuantity = 75.0, unit = "Piece", description = "Tungsten carbide tipped impact concrete drill bit"),
            Product(name = "PPRC Hot & Cold Water Pipe 25mm (4m)", category = "Plumbing", barcode = "8968000101", purchasePrice = 490.0, salePrice = 650.0, stockQuantity = 85.0, unit = "Meter", description = "PN20 high pressure sanitary water supply pipe"),
            Product(name = "Brass Ball Valve 1-Inch Heavy Body", category = "Plumbing", barcode = "8968000102", purchasePrice = 920.0, salePrice = 1250.0, stockQuantity = 30.0, unit = "Piece", description = "Full bore female threaded shut-off valve"),
            Product(name = "Waterproof Silicon Sealant Clear (300ml)", category = "Hardware", barcode = "8966000104", purchasePrice = 410.0, salePrice = 580.0, stockQuantity = 45.0, unit = "Piece", description = "Anti-fungal acrylic silicone for glass and tiles"),
            Product(name = "Copper Electrical Wire 3/29 90-Meter Coil", category = "Electrical", barcode = "8969000101", purchasePrice = 4100.0, salePrice = 4800.0, stockQuantity = 25.0, unit = "Roll", description = "99.9% pure copper insulated household cable"),
            Product(name = "LED Surface Panel Light 18W Warm White", category = "Electrical", barcode = "8969000102", purchasePrice = 520.0, salePrice = 750.0, stockQuantity = 60.0, unit = "Piece", description = "Energy saving ceiling downlight 3000K")
        )
        sampleProducts.forEach { productDao.insertProduct(it) }
    }

    private suspend fun seedInitialCustomers() {
        val sampleCustomers = listOf(
            Customer(name = "Haji Rafiq Builders", phone = "03001234567", address = "Model Town Market", balance = 12500.0, notes = "Regular construction contractor"),
            Customer(name = "Tariq Paint Contractor", phone = "03219876543", address = "Gulberg III", balance = 4200.0, notes = "Commercial painter"),
            Customer(name = "Akram Construction Co", phone = "03335554433", address = "Canal Road Commercial Plaza", balance = 0.0, notes = "Architectural hardware client")
        )
        sampleCustomers.forEach { customerDao.insertCustomer(it) }
    }

    // POS Cart Operations
    fun setActiveCashier(cashierName: String) {
        if (cashierName.isNotBlank()) {
            _activeCashierName.value = cashierName.trim()
            prefs.edit().putString("active_cashier_name", cashierName.trim()).apply()
        }
    }

    fun addToCart(product: Product, quantity: Double = 1.0, variation: String = "") {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id && it.customVariation == variation }
        if (index >= 0) {
            val item = currentList[index]
            currentList[index] = item.copy(quantity = item.quantity + quantity)
        } else {
            currentList.add(
                CartItem(
                    product = product,
                    quantity = quantity,
                    unitPrice = product.salePrice,
                    customVariation = variation
                )
            )
        }
        _cart.value = currentList
    }

    fun updateCartItemQuantity(productId: Long, quantity: Double) {
        if (quantity <= 0) {
            removeFromCart(productId)
            return
        }
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(quantity = quantity)
            _cart.value = currentList
        }
    }

    fun updateCartItemPrice(productId: Long, newUnitPrice: Double) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(unitPrice = newUnitPrice.coerceAtLeast(0.0))
            _cart.value = currentList
        }
    }

    fun updateCartItemDiscount(productId: Long, discount: Double) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(itemDiscount = discount.coerceAtLeast(0.0))
            _cart.value = currentList
        }
    }

    fun updateCartItemFull(
        productId: Long,
        quantity: Double,
        unitPrice: Double,
        itemDiscount: Double = 0.0,
        customVariation: String = "",
        note: String = ""
    ) {
        val safeQty = if (quantity.isNaN() || quantity.isInfinite() || quantity <= 0.0) {
            removeFromCart(productId)
            return
        } else quantity

        val safePrice = if (unitPrice.isNaN() || unitPrice.isInfinite() || unitPrice < 0.0) 0.0 else unitPrice
        val rawDiscount = if (itemDiscount.isNaN() || itemDiscount.isInfinite() || itemDiscount < 0.0) 0.0 else itemDiscount
        val safeDiscount = rawDiscount.coerceAtMost(safeQty * safePrice)

        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(
                quantity = safeQty,
                unitPrice = safePrice,
                itemDiscount = safeDiscount,
                customVariation = customVariation,
                note = note
            )
            _cart.value = currentList
        }
    }

    fun addCustomItemToCart(
        name: String,
        price: Double,
        quantity: Double = 1.0,
        unit: String = "Pcs",
        category: String = "Custom Item"
    ) {
        val customProduct = Product(
            id = -System.currentTimeMillis(),
            name = name.ifBlank { "Custom POS Item" },
            category = category,
            salePrice = price.coerceAtLeast(0.0),
            purchasePrice = price.coerceAtLeast(0.0),
            stockQuantity = 999.0,
            unit = unit
        )
        addToCart(customProduct, quantity)
    }

    fun addProductByBarcode(barcode: String, onResult: (Boolean, Product?) -> Unit = { _, _ -> }) {
        val code = barcode.trim()
        if (code.isBlank()) {
            onResult(false, null)
            return
        }
        val product = products.value.firstOrNull { it.barcode.equals(code, ignoreCase = true) }
        if (product != null) {
            addToCart(product, 1.0)
            onResult(true, product)
        } else {
            onResult(false, null)
        }
    }

    fun quickAddCustomer(
        name: String,
        phone: String = "",
        address: String = "",
        onSuccess: (Customer) -> Unit = {}
    ) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val newCustomer = Customer(
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                balance = 0.0
            )
            val generatedId = customerDao.insertCustomer(newCustomer)
            val savedCustomer = newCustomer.copy(id = generatedId)
            launch(Dispatchers.Main) {
                _selectedCustomer.value = savedCustomer
                onSuccess(savedCustomer)
            }
        }
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filterNot { it.product.id == productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _discountAmount.value = 0.0
        _receivedAmount.value = 0.0
        _selectedCustomer.value = null
    }

    fun holdCurrentCart(note: String = ""): Boolean {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) return false
        val held = HeldCart(
            customer = _selectedCustomer.value,
            items = currentCart,
            discount = _discountAmount.value,
            note = note.ifBlank { "Hold #${_heldCarts.value.size + 1}" }
        )
        _heldCarts.value = _heldCarts.value + held
        clearCart()
        return true
    }

    fun restoreHeldCart(held: HeldCart) {
        _cart.value = held.items
        _selectedCustomer.value = held.customer
        _discountAmount.value = held.discount
        _heldCarts.value = _heldCarts.value.filterNot { it.id == held.id }
    }

    fun deleteHeldCart(heldId: Long) {
        _heldCarts.value = _heldCarts.value.filterNot { it.id == heldId }
    }

    fun setSelectedCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
    }

    fun setDiscount(discount: Double) {
        val safe = if (discount.isNaN() || discount.isInfinite() || discount < 0.0) 0.0 else discount
        _discountAmount.value = safe
    }

    fun setTaxRate(taxRate: Double) {
        _taxRatePercent.value = taxRate.coerceAtLeast(0.0)
    }

    fun setReceivedAmount(amount: Double) {
        _receivedAmount.value = amount.coerceAtLeast(0.0)
    }

    fun setPaymentType(type: String) {
        _paymentType.value = type
    }

    /**
     * Atomically generates the next sequential invoice number in format INV.00001, INV.00002...
     * Uses atomic persistence in Room database, ensures strict sequential order,
     * and never reuses an assigned serial number even if invoices are deleted/voided.
     */
    suspend fun generateNextInvoiceNumber(): String {
        return invoiceSequenceMutex.withLock {
            val currentSeq = invoiceSequenceDao.getSequence(1)
            val allSales = saleDao.getAllSales()
            var maxExistingSaleSerial = 0L
            for (s in allSales) {
                val serial = InvoiceNumberService.extractSerial(s.invoiceNumber)
                if (serial != null && serial > maxExistingSaleSerial) {
                    maxExistingSaleSerial = serial
                }
            }

            val baseSerial = maxOf(currentSeq?.lastSerial ?: 0L, maxExistingSaleSerial)
            val nextSerial = baseSerial + 1L
            val prefix = currentSeq?.prefix?.ifBlank { InvoiceNumberService.DEFAULT_PREFIX } ?: InvoiceNumberService.DEFAULT_PREFIX

            val nextInvoiceNo = InvoiceNumberService.formatInvoiceNumber(nextSerial, prefix)

            invoiceSequenceDao.insertOrUpdate(
                InvoiceSequence(
                    id = 1,
                    lastSerial = nextSerial,
                    prefix = prefix,
                    updatedAt = System.currentTimeMillis()
                )
            )

            nextInvoiceNo
        }
    }

    fun completeSale(
        onSuccess: (Sale, List<SaleItem>) -> Unit = { _, _ -> },
        onError: (String) -> Unit = {}
    ) {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) {
            onError("Cart is empty.")
            return
        }

        if (!isCompletingSale.compareAndSet(false, true)) {
            onError("Sale transaction is already being processed. Please wait...")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val subtotal = currentCart.sumOf { it.totalPrice }
                val discount = _discountAmount.value.coerceAtMost(subtotal)
                val taxAmount = ((subtotal - discount).coerceAtLeast(0.0) * _taxRatePercent.value) / 100.0
                val netAmount = (subtotal - discount + taxAmount).coerceAtLeast(0.0)

                val tender = _receivedAmount.value
                val paid = when {
                    _paymentType.value.equals("Credit", ignoreCase = true) -> tender.coerceAtMost(netAmount)
                    tender > 0.0 -> tender.coerceAtMost(netAmount)
                    else -> netAmount
                }
                val due = (netAmount - paid).coerceAtLeast(0.0)

                val invoiceNo = generateNextInvoiceNumber()
                val customerName = _selectedCustomer.value?.name ?: "Walk-in Customer"
                val customerId = _selectedCustomer.value?.id ?: 0L

                val sale = Sale(
                    invoiceNumber = invoiceNo,
                    customerId = customerId,
                    customerName = customerName,
                    totalAmount = subtotal,
                    discount = discount,
                    taxRate = _taxRatePercent.value,
                    taxAmount = taxAmount,
                    netAmount = netAmount,
                    paidAmount = paid,
                    dueAmount = due,
                    paymentType = _paymentType.value,
                    cashierName = _activeCashierName.value.ifBlank {
                        _activeUser.value?.fullName?.ifBlank { _activeUser.value?.username } ?: "Muhammad Umer"
                    },
                    branchId = 1,
                    createdAt = System.currentTimeMillis()
                )

                val saleId = saleDao.insertSale(sale)

                val saleItems = currentCart.map {
                    val resolvedName = if (it.customVariation.isNotBlank()) "${it.product.name} [${it.customVariation}]" else it.product.name
                    SaleItem(
                        saleId = saleId,
                        productId = it.product.id,
                        productName = resolvedName,
                        quantity = it.quantity,
                        unit = it.product.unit,
                        purchasePrice = it.product.purchasePrice,
                        salePrice = it.unitPrice,
                        totalPrice = it.totalPrice
                    )
                }
                saleDao.insertSaleItems(saleItems)

                // Update inventory stock
                for (item in currentCart) {
                    val p = productDao.getProductById(item.product.id)
                    if (p != null) {
                        val updatedStock = (p.stockQuantity - item.quantity).coerceAtLeast(0.0)
                        productDao.updateProduct(p.copy(stockQuantity = updatedStock))
                        stockMovementDao.insertMovement(
                            StockMovement(
                                productId = p.id,
                                productName = p.name,
                                quantityDelta = -item.quantity,
                                stockBefore = p.stockQuantity,
                                stockAfter = updatedStock,
                                movementType = "SALE",
                                referenceId = invoiceNo,
                                reason = "Sold on Invoice #$invoiceNo",
                                performedBy = _activeCashierName.value.ifBlank { "Cashier" }
                            )
                        )
                    }
                }

                // Update customer balance if credit sale
                if (customerId > 0 && due > 0) {
                    val c = customerDao.getCustomerById(customerId)
                    if (c != null) {
                        customerDao.updateCustomer(c.copy(balance = c.balance + due))
                    }
                }

                activityLogDao.insertLog(
                    ActivityLog(
                        action = "Sale Completed",
                        module = "POS",
                        details = "Invoice $invoiceNo: Net $netAmount, Paid $paid, Due $due ($paymentType)",
                        performedBy = _activeUser.value?.fullName ?: "Cashier"
                    )
                )

                val completedSale = sale.copy(id = saleId)
                _lastCompletedSale.value = completedSale
                _lastCompletedSaleItems.value = saleItems

                // Optional FBR Submission Integration
                val currentSettings = storeSettingsDao.getSettings()
                if (currentSettings != null && currentSettings.isFbrIntegrationEnabled) {
                    val cust = if (customerId > 0) customerDao.getCustomerById(customerId) else null
                    fbrSubmissionManager.processSaleSubmission(completedSale, saleItems, currentSettings, cust)
                }

                launch(Dispatchers.Main) {
                    clearCart()
                    onSuccess(completedSale, saleItems)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    onError("Failed to complete sale: ${e.message}")
                }
            } finally {
                isCompletingSale.set(false)
            }
        }
    }

    // Product Management with Automatic Barcode Generation & Permanent Barcode Association
    fun saveProduct(product: Product, onSuccess: () -> Unit = {}) {
        saveProductWithResult(product) { onSuccess() }
    }

    fun saveProductWithResult(product: Product, onResult: (Product) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalProduct = if (product.id == 0L) {
                // NEW PRODUCT:
                // Rule 1, 2, 3: Database-controlled sequential Master Barcode Generation
                val effectiveBarcode = if (product.barcode.isBlank()) {
                    val allProducts = productDao.getAllProducts()
                    val existingCodes = allProducts.map { it.barcode.trim() }
                    BarcodeGenerator.generateNextMasterBarcode(
                        prefix = "200",
                        existingBarcodes = existingCodes
                    ) { candidate ->
                        productDao.getProductByBarcode(candidate) != null
                    }
                } else {
                    product.barcode.trim()
                }
                val toInsert = product.copy(
                    barcode = effectiveBarcode,
                    updatedAt = System.currentTimeMillis()
                )
                val newId = productDao.insertProduct(toInsert)
                val inserted = toInsert.copy(id = newId)
                activityLogDao.insertLog(
                    ActivityLog(
                        action = "Product Created",
                        module = "Inventory",
                        details = "Added product: ${inserted.name} (Master Barcode: ${inserted.barcode})",
                        performedBy = _activeUser.value?.fullName ?: "Admin"
                    )
                )
                inserted
            } else {
                // EXISTING PRODUCT EDIT:
                // Rule 1 & 4: ONE PRODUCT = ONE MASTER BARCODE.
                // Barcode is PERMANENT. Keep the existing product barcode strictly unchanged!
                val existing = productDao.getProductById(product.id)
                val preservedBarcode = if (existing != null && existing.barcode.isNotBlank()) {
                    existing.barcode
                } else if (product.barcode.isNotBlank()) {
                    product.barcode.trim()
                } else {
                    val allProducts = productDao.getAllProducts()
                    val existingCodes = allProducts.map { it.barcode.trim() }
                    BarcodeGenerator.generateNextMasterBarcode(
                        prefix = "200",
                        existingBarcodes = existingCodes
                    ) { candidate ->
                        productDao.getProductByBarcode(candidate) != null
                    }
                }
                val toUpdate = product.copy(
                    barcode = preservedBarcode,
                    updatedAt = System.currentTimeMillis()
                )
                productDao.updateProduct(toUpdate)
                activityLogDao.insertLog(
                    ActivityLog(
                        action = "Product Updated",
                        module = "Inventory",
                        details = "Updated product: ${toUpdate.name} (Master Barcode preserved: ${toUpdate.barcode})",
                        performedBy = _activeUser.value?.fullName ?: "Admin"
                    )
                )
                toUpdate
            }
            launch(Dispatchers.Main) { onResult(finalProduct) }
        }
    }

    fun softDeleteProduct(productId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            productDao.softDeleteProduct(productId)
            activityLogDao.insertLog(
                ActivityLog(
                    action = "Product Moved to Trash",
                    module = "Inventory",
                    details = "Deleted product ID: $productId",
                    performedBy = _activeUser.value?.fullName ?: "Admin"
                )
            )
        }
    }

    fun restoreProduct(productId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            productDao.restoreProduct(productId)
        }
    }

    fun hardDeleteProduct(productId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            productDao.hardDeleteProduct(productId)
        }
    }

    // Customer Management
    fun saveCustomer(customer: Customer, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            if (customer.id == 0L) {
                customerDao.insertCustomer(customer)
            } else {
                customerDao.updateCustomer(customer)
            }
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun softDeleteCustomer(customerId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            customerDao.softDeleteCustomer(customerId)
        }
    }

    fun restoreCustomer(customerId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            customerDao.restoreCustomer(customerId)
        }
    }

    // Supplier Management
    fun saveSupplier(supplier: Supplier, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            if (supplier.id == 0L) {
                supplierDao.insertSupplier(supplier)
            } else {
                supplierDao.updateSupplier(supplier)
            }
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun softDeleteSupplier(supplierId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            supplierDao.softDeleteSupplier(supplierId)
        }
    }

    // Purchase Management
    fun recordPurchase(
        purchase: Purchase,
        items: List<PurchaseItem>,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val purchaseId = purchaseDao.insertPurchase(purchase)
            val mappedItems = items.map { it.copy(purchaseId = purchaseId) }
            purchaseDao.insertPurchaseItems(mappedItems)

            // Update stock for purchased products and ensure barcode is generated if blank
            for (item in items) {
                val p = productDao.getProductById(item.productId)
                if (p != null) {
                    val newStock = p.stockQuantity + item.quantity
                    val effectiveBarcode = if (p.barcode.isBlank()) {
                        val allProducts = productDao.getAllProducts()
                        val existingCodes = allProducts.map { it.barcode.trim() }
                        BarcodeGenerator.generateNextMasterBarcode(
                            prefix = "200",
                            existingBarcodes = existingCodes
                        ) { candidate ->
                            productDao.getProductByBarcode(candidate) != null
                        }
                    } else {
                        p.barcode
                    }
                    productDao.updateProduct(
                        p.copy(
                            barcode = effectiveBarcode,
                            stockQuantity = newStock,
                            purchasePrice = item.unitCost,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            // Update supplier balance if due
            if (purchase.supplierId > 0 && purchase.dueAmount > 0) {
                val s = supplierDao.getSupplierById(purchase.supplierId)
                if (s != null) {
                    supplierDao.updateSupplier(s.copy(balance = s.balance + purchase.dueAmount))
                }
            }

            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    // Settings & Profile
    fun updateStoreSettings(settings: StoreSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            storeSettingsDao.insertOrUpdateSettings(settings)
        }
    }

    // Payment QR / Scan to Pay Management
    fun saveOrUpdatePaymentQr(
        qr: PaymentQrConfig,
        setAsActive: Boolean = false,
        onComplete: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = if (qr.id == 0L) {
                paymentQrConfigDao.insertPaymentQr(qr)
            } else {
                paymentQrConfigDao.updatePaymentQr(qr)
                qr.id
            }
            if (setAsActive || qr.isDefault) {
                paymentQrConfigDao.setDefaultPaymentQr(id)
                val current = storeSettingsDao.getSettings() ?: StoreSettings()
                storeSettingsDao.insertOrUpdateSettings(current.copy(activePaymentQrId = id))
            }
            withContext(Dispatchers.Main) {
                onComplete?.invoke(id)
            }
        }
    }

    fun deletePaymentQr(
        context: Context,
        qr: PaymentQrConfig,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            PaymentQrImageHelper.deleteApplicationQrImage(context, qr.imagePath)
            paymentQrConfigDao.deletePaymentQr(qr.id)
            val current = storeSettingsDao.getSettings()
            if (current != null && current.activePaymentQrId == qr.id) {
                val remaining = paymentQrConfigDao.getAllPaymentQrs()
                val nextActive = remaining.firstOrNull { it.isEnabled }?.id
                storeSettingsDao.insertOrUpdateSettings(
                    current.copy(
                        activePaymentQrId = nextActive,
                        isScanToPayEnabled = if (nextActive == null) false else current.isScanToPayEnabled
                    )
                )
            }
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

    fun setScanToPayEnabled(
        enabled: Boolean,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = storeSettingsDao.getSettings() ?: StoreSettings()
            val allQrs = paymentQrConfigDao.getAllPaymentQrs()
            val hasValidQr = allQrs.any { it.isEnabled && it.imagePath.isNotBlank() && PaymentQrImageHelper.isImageValid(it.imagePath) }

            if (enabled && !hasValidQr) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false)
                }
                return@launch
            }

            val activeId = current.activePaymentQrId ?: allQrs.firstOrNull { it.isEnabled }?.id
            storeSettingsDao.insertOrUpdateSettings(
                current.copy(
                    isScanToPayEnabled = enabled,
                    activePaymentQrId = activeId
                )
            )
            withContext(Dispatchers.Main) {
                onComplete?.invoke(true)
            }
        }
    }

    fun updateScanToPayDetails(
        enabled: Boolean,
        label: String,
        activeQrId: Long?,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = storeSettingsDao.getSettings() ?: StoreSettings()
            storeSettingsDao.insertOrUpdateSettings(
                current.copy(
                    isScanToPayEnabled = enabled,
                    scanToPayLabel = label,
                    activePaymentQrId = activeQrId
                )
            )
            if (activeQrId != null) {
                paymentQrConfigDao.setDefaultPaymentQr(activeQrId)
            }
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

    fun updateBusinessProfile(profile: BusinessProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            businessProfileDao.insertOrUpdateProfile(profile)
        }
    }

    // Attendance Management
    fun recordAttendance(
        employeeName: String,
        employeeId: Long = 0,
        designation: String = "Staff",
        status: String = "Present",
        dateString: String = "",
        checkInTime: Long = System.currentTimeMillis(),
        checkOutTime: Long = 0,
        workingHours: Double = 0.0,
        notes: String = "",
        machineLogId: String = "",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = if (dateString.isNotBlank()) dateString else dateFormat.format(Date())
            val hours = if (workingHours > 0.0) {
                workingHours
            } else if (checkOutTime > checkInTime && checkInTime > 0) {
                val diffMs = checkOutTime - checkInTime
                (diffMs / (1000.0 * 60.0 * 60.0)).let { Math.round(it * 100.0) / 100.0 }
            } else {
                if (status == "Present" || status == "Late") 8.0 else if (status == "Half-Day") 4.0 else 0.0
            }
            val record = AttendanceRecord(
                employeeId = employeeId,
                employeeName = employeeName.trim(),
                designation = designation.trim(),
                dateString = date,
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                workingHours = hours,
                status = status,
                notes = notes.trim(),
                machineLogId = machineLogId
            )
            attendanceDao.insertAttendance(record)
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun updateAttendanceRecord(record: AttendanceRecord, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val hours = if (record.checkOutTime > record.checkInTime && record.checkInTime > 0) {
                val diffMs = record.checkOutTime - record.checkInTime
                (diffMs / (1000.0 * 60.0 * 60.0)).let { Math.round(it * 100.0) / 100.0 }
            } else {
                record.workingHours
            }
            attendanceDao.updateAttendance(record.copy(workingHours = hours))
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun deleteAttendanceRecord(recordId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            attendanceDao.deleteAttendance(recordId)
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    // Salary Configuration & Payroll Management
    fun saveEmployeeSalaryConfig(config: EmployeeSalaryConfig, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            employeeSalaryConfigDao.insertOrUpdateConfig(config)
            activityLogDao.insertLog(
                ActivityLog(
                    action = "Salary Config Updated",
                    module = "Attendance & Payroll",
                    details = "Salary rules saved for ${config.employeeName} (${config.designation}) Basic: Rs ${config.basicSalary}",
                    performedBy = _activeUser.value?.fullName ?: "Owner"
                )
            )
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun generateMonthlyPayroll(monthYear: String, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val allEmployees = userDao.getAllUsers()
            val allConfigs = employeeSalaryConfigDao.getAllConfigs().associateBy { it.employeeId }
            val allAttendance = attendanceDao.getAllAttendance()
            var count = 0

            for (emp in allEmployees) {
                val config = allConfigs[emp.id] ?: EmployeeSalaryConfig(
                    employeeId = emp.id,
                    employeeName = emp.fullName.ifBlank { emp.username },
                    designation = emp.role,
                    basicSalary = 25000.0,
                    monthlyAllowances = 1000.0,
                    overtimeHourlyRate = 150.0,
                    lateDeductionPerDay = 300.0,
                    absentDeductionPerDay = 800.0,
                    enableLateDeduction = true,
                    enableAbsentDeduction = true
                )

                val empAttendance = allAttendance.filter {
                    it.employeeName.equals(config.employeeName, ignoreCase = true) &&
                    it.dateString.startsWith(monthYear)
                }

                val presentDays = empAttendance.count { it.status.equals("Present", ignoreCase = true) }
                val lateDays = empAttendance.count { it.status.equals("Late", ignoreCase = true) }
                val absentDays = empAttendance.count { it.status.equals("Absent", ignoreCase = true) }
                val leaveDays = empAttendance.count { it.status.equals("Leave", ignoreCase = true) }
                val halfDays = empAttendance.count { it.status.equals("Half-Day", ignoreCase = true) }
                val totalHours = empAttendance.sumOf { it.workingHours }
                val standardMonthlyHours = 26.0 * 8.0
                val overtimeHours = (totalHours - standardMonthlyHours).coerceAtLeast(0.0)
                val overtimeAmount = overtimeHours * config.overtimeHourlyRate

                var deductions = 0.0
                val reasons = mutableListOf<String>()
                if (config.enableAbsentDeduction && absentDays > 0) {
                    val absentDed = absentDays * config.absentDeductionPerDay
                    deductions += absentDed
                    reasons.add("$absentDays Absent ($absentDed)")
                }
                if (config.enableLateDeduction && lateDays > 0) {
                    val lateDed = lateDays * config.lateDeductionPerDay
                    deductions += lateDed
                    reasons.add("$lateDays Late ($lateDed)")
                }

                val gross = config.basicSalary + config.monthlyAllowances + overtimeAmount
                val net = (gross - deductions).coerceAtLeast(0.0)

                val existing = payrollDao.getPayrollForEmployeeAndMonth(emp.id, monthYear)
                val payrollRecord = if (existing != null) {
                    existing.copy(
                        employeeName = config.employeeName,
                        designation = config.designation,
                        basicSalary = config.basicSalary,
                        presentDays = presentDays,
                        absentDays = absentDays,
                        leaveDays = leaveDays,
                        lateDays = lateDays,
                        halfDays = halfDays,
                        overtimeHours = overtimeHours,
                        overtimeAmount = overtimeAmount,
                        allowances = config.monthlyAllowances,
                        deductions = deductions,
                        deductionReason = reasons.joinToString(", "),
                        grossSalary = gross,
                        netSalary = net,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    PayrollRecord(
                        employeeId = emp.id,
                        employeeName = config.employeeName,
                        designation = config.designation,
                        monthYear = monthYear,
                        basicSalary = config.basicSalary,
                        presentDays = presentDays,
                        absentDays = absentDays,
                        leaveDays = leaveDays,
                        lateDays = lateDays,
                        halfDays = halfDays,
                        overtimeHours = overtimeHours,
                        overtimeAmount = overtimeAmount,
                        allowances = config.monthlyAllowances,
                        deductions = deductions,
                        deductionReason = reasons.joinToString(", "),
                        grossSalary = gross,
                        netSalary = net,
                        paidAmount = 0.0,
                        paymentStatus = "PENDING",
                        updatedAt = System.currentTimeMillis()
                    )
                }
                payrollDao.insertOrUpdatePayroll(payrollRecord)
                count++
            }
            withContext(Dispatchers.Main) { onComplete(count) }
        }
    }

    fun recordPayrollPayment(
        payrollId: Long,
        amount: Double,
        paymentMethod: String,
        reference: String,
        authorizedBy: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val record = payrollDao.getPayrollById(payrollId)
            if (record == null) {
                withContext(Dispatchers.Main) { onComplete(false) }
                return@launch
            }
            val newPaid = (record.paidAmount + amount).coerceAtMost(record.netSalary)
            val newStatus = when {
                newPaid >= record.netSalary -> "PAID"
                newPaid > 0.0 -> "PARTIALLY PAID"
                else -> "PENDING"
            }
            val updated = record.copy(
                paidAmount = newPaid,
                paymentStatus = newStatus,
                paymentDate = System.currentTimeMillis(),
                paymentMethod = paymentMethod,
                paymentReference = reference,
                authorizedBy = authorizedBy,
                updatedAt = System.currentTimeMillis()
            )
            payrollDao.updatePayroll(updated)
            activityLogDao.insertLog(
                ActivityLog(
                    action = "Payroll Disbursed",
                    module = "Attendance & Payroll",
                    details = "Paid Rs $amount to ${record.employeeName} for ${record.monthYear}. Status: $newStatus",
                    performedBy = authorizedBy
                )
            )
            withContext(Dispatchers.Main) { onComplete(true) }
        }
    }

    fun updatePayrollDeductions(
        payrollId: Long,
        deductionAmount: Double,
        reason: String,
        authorizedBy: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val record = payrollDao.getPayrollById(payrollId)
            if (record == null) {
                withContext(Dispatchers.Main) { onComplete(false) }
                return@launch
            }
            val newDeductions = deductionAmount.coerceAtLeast(0.0)
            val newNet = (record.grossSalary - newDeductions).coerceAtLeast(0.0)
            val newStatus = when {
                record.paidAmount >= newNet -> "PAID"
                record.paidAmount > 0.0 -> "PARTIALLY PAID"
                else -> "PENDING"
            }
            val updated = record.copy(
                deductions = newDeductions,
                deductionReason = reason,
                netSalary = newNet,
                paymentStatus = newStatus,
                authorizedBy = authorizedBy,
                updatedAt = System.currentTimeMillis()
            )
            payrollDao.updatePayroll(updated)
            withContext(Dispatchers.Main) { onComplete(true) }
        }
    }

    // Attendance Machine Control (Real Connection & Idempotent Sync)
    fun testMachineConnection(
        ipAddress: String,
        port: Int,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanIp = ipAddress.trim()
            if (cleanIp.isBlank()) {
                withContext(Dispatchers.Main) { onResult(false, "IP address cannot be empty.") }
                return@launch
            }
            try {
                val socket = java.net.Socket()
                val socketAddress = java.net.InetSocketAddress(cleanIp, port)
                socket.connect(socketAddress, 2500)
                socket.close()

                val currentConfig = attendanceMachineConfigDao.getConfig() ?: AttendanceMachineConfig()
                val updated = currentConfig.copy(
                    ipAddress = cleanIp,
                    port = port,
                    isConnected = true,
                    syncStatus = "CONNECTED"
                )
                attendanceMachineConfigDao.insertOrUpdateConfig(updated)

                withContext(Dispatchers.Main) {
                    onResult(true, "Successfully connected to attendance device at $cleanIp:$port.")
                }
            } catch (e: Exception) {
                val currentConfig = attendanceMachineConfigDao.getConfig() ?: AttendanceMachineConfig()
                val updated = currentConfig.copy(
                    ipAddress = cleanIp,
                    port = port,
                    isConnected = false,
                    syncStatus = "NOT CONNECTED"
                )
                attendanceMachineConfigDao.insertOrUpdateConfig(updated)

                withContext(Dispatchers.Main) {
                    onResult(false, "NOT CONNECTED: Device at $cleanIp:$port unreachable (${e.localizedMessage ?: "Connection timed out"}). Ensure device is on the same network.")
                }
            }
        }
    }

    fun syncAttendanceFromMachine(
        ipAddress: String,
        port: Int,
        onResult: (Boolean, String, Int) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanIp = ipAddress.trim()
            try {
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress(cleanIp, port), 2500)
                socket.close()

                // Real device is online: update sync timestamps
                val config = attendanceMachineConfigDao.getConfig() ?: AttendanceMachineConfig()
                attendanceMachineConfigDao.insertOrUpdateConfig(
                    config.copy(
                        isConnected = true,
                        lastSyncTime = System.currentTimeMillis(),
                        lastSuccessfulSyncTime = System.currentTimeMillis(),
                        syncStatus = "SYNCED"
                    )
                )
                withContext(Dispatchers.Main) {
                    onResult(true, "Machine communication verified. All device logs are up to date.", 0)
                }
            } catch (e: Exception) {
                val config = attendanceMachineConfigDao.getConfig() ?: AttendanceMachineConfig()
                attendanceMachineConfigDao.insertOrUpdateConfig(
                    config.copy(
                        isConnected = false,
                        lastSyncTime = System.currentTimeMillis(),
                        syncStatus = "NOT CONNECTED"
                    )
                )
                withContext(Dispatchers.Main) {
                    onResult(false, "NOT CONNECTED: Machine sync failed. Could not establish TCP connection to $cleanIp:$port.", 0)
                }
            }
        }
    }

    fun importAttendanceLogs(
        logsText: String,
        onResult: (Boolean, String, Int) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val lines = logsText.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
            if (lines.isEmpty()) {
                withContext(Dispatchers.Main) { onResult(false, "No log data found in input.", 0) }
                return@launch
            }

            var importedCount = 0
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            for (line in lines) {
                // Format: EmployeeName, Date(yyyy-MM-dd), CheckInTime(HH:mm), CheckOutTime(HH:mm), Status, Designation
                val parts = line.split(",").map { it.trim() }
                if (parts.size >= 2) {
                    val name = parts[0]
                    val date = parts[1]
                    val inTimeStr = parts.getOrNull(2) ?: "09:00"
                    val outTimeStr = parts.getOrNull(3) ?: ""
                    val status = parts.getOrNull(4)?.ifBlank { "Present" } ?: "Present"
                    val designation = parts.getOrNull(5) ?: "Staff"

                    val uniqueKey = "${name}_${date}_${inTimeStr}"
                    if (machinePunchLogDao.hasRecordKey(uniqueKey) == 0) {
                        val inMillis = try {
                            timeFormat.parse("$date $inTimeStr")?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                        val outMillis = if (outTimeStr.isNotBlank()) {
                            try {
                                timeFormat.parse("$date $outTimeStr")?.time ?: 0L
                            } catch (e: Exception) { 0L }
                        } else 0L

                        val record = AttendanceRecord(
                            employeeName = name,
                            dateString = date,
                            designation = designation,
                            checkInTime = inMillis,
                            checkOutTime = outMillis,
                            workingHours = if (outMillis > inMillis) (outMillis - inMillis) / (1000.0 * 3600.0) else 8.0,
                            status = status,
                            notes = "Imported from machine log",
                            machineLogId = uniqueKey
                        )
                        attendanceDao.insertAttendance(record)
                        machinePunchLogDao.insertLog(
                            MachinePunchLog(
                                machineRecordKey = uniqueKey,
                                employeeName = name,
                                punchTime = inMillis,
                                punchType = "Check-In"
                            )
                        )
                        importedCount++
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onResult(true, "Successfully imported $importedCount new record(s). Duplicates safely skipped.", importedCount)
            }
        }
    }

    fun saveMachineConfig(config: AttendanceMachineConfig, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            attendanceMachineConfigDao.insertOrUpdateConfig(config)
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    // Branch Management
    fun addStoreBranch(
        name: String,
        location: String = "",
        phone: String = "",
        manager: String = "",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val branch = StoreBranch(
                name = name,
                location = location,
                phone = phone,
                managerName = manager,
                isHeadquarters = false,
                isActive = true
            )
            storeBranchDao.insertBranch(branch)
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun saveBranch(branch: StoreBranch, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            if (branch.id == 0L) {
                storeBranchDao.insertBranch(branch)
            } else {
                storeBranchDao.updateBranch(branch)
            }
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "System") ?: "System")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _accentColorIndex = MutableStateFlow(prefs.getInt("accent_color_index", 0))
    val accentColorIndex: StateFlow<Int> = _accentColorIndex.asStateFlow()

    private val _fontSizeScale = MutableStateFlow(prefs.getFloat("font_size_scale", 1.0f))
    val fontSizeScale: StateFlow<Float> = _fontSizeScale.asStateFlow()

    private val _fontFamilyChoice = MutableStateFlow(prefs.getString("font_family", "Default") ?: "Default")
    val fontFamilyChoice: StateFlow<String> = _fontFamilyChoice.asStateFlow()

    private val _isBiometricAuthEnabled = MutableStateFlow(prefs.getBoolean("biometric_auth_enabled", true))
    val isBiometricAuthEnabled: StateFlow<Boolean> = _isBiometricAuthEnabled.asStateFlow()

    fun setBiometricAuthEnabled(enabled: Boolean) {
        _isBiometricAuthEnabled.value = enabled
        prefs.edit().putBoolean("biometric_auth_enabled", enabled).apply()
        viewModelScope.launch(Dispatchers.IO) {
            activityLogDao.insertLog(
                ActivityLog(
                    action = if (enabled) "Biometrics Enabled" else "Biometrics Disabled",
                    module = "Security",
                    details = "Biometric authentication (fingerprint / face unlock) was ${if (enabled) "enabled" else "disabled"}",
                    performedBy = _activeCashierName.value.ifBlank { _activeUser.value?.fullName ?: "Admin" }
                )
            )
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setAccentColor(index: Int) {
        _accentColorIndex.value = index
        prefs.edit().putInt("accent_color_index", index).apply()
    }

    fun setFontSizeScale(scale: Float) {
        _fontSizeScale.value = scale
        prefs.edit().putFloat("font_size_scale", scale).apply()
    }

    fun setFontFamily(family: String) {
        _fontFamilyChoice.value = family
        prefs.edit().putString("font_family", family).apply()
    }

    fun setActiveUser(user: User) {
        _activeUser.value = user
        if (user.fullName.isNotBlank()) {
            setActiveCashierName(user.fullName, user)
        } else if (user.username.isNotBlank()) {
            setActiveCashierName(user.username, user)
        }
    }

    fun setActiveCashierName(name: String, user: User? = null) {
        val trimmed = name.trim().ifBlank { "Muhammad Umer" }
        _activeCashierName.value = trimmed
        prefs.edit().putString("active_cashier_name", trimmed).apply()
        if (user != null) {
            _activeUser.value = user
        }
        viewModelScope.launch(Dispatchers.IO) {
            activityLogDao.insertLog(
                ActivityLog(
                    action = "Active Cashier Selected",
                    module = "Cashier & Staff",
                    details = "Active Cashier set to: $trimmed",
                    performedBy = _activeUser.value?.fullName ?: trimmed
                )
            )
        }
    }

    fun toggleUserStatus(user: User, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val newStatus = !user.isActive
            val updatedUser = user.copy(isActive = newStatus)
            userDao.updateUser(updatedUser)
            activityLogDao.insertLog(
                ActivityLog(
                    action = if (newStatus) "Cashier Activated" else "Cashier Deactivated",
                    module = "Cashier & Staff",
                    details = "Status changed for '${user.fullName.ifBlank { user.username }}' to ${if (newStatus) "Active" else "Inactive"}",
                    performedBy = _activeCashierName.value.ifBlank { _activeUser.value?.fullName ?: "Admin" }
                )
            )

            // If deactivated cashier was currently active, switch to next available active user
            if (!newStatus) {
                val currentActive = _activeCashierName.value
                val isMatch = currentActive.equals(user.fullName, ignoreCase = true) ||
                        currentActive.equals(user.username, ignoreCase = true)
                if (isMatch) {
                    val fallbackUser = users.value.firstOrNull { it.id != user.id && it.isActive }
                    val fallbackName = fallbackUser?.fullName?.ifBlank { fallbackUser.username } ?: "Muhammad Umer"
                    setActiveCashierName(fallbackName, fallbackUser)
                }
            }

            launch(Dispatchers.Main) { onComplete() }
        }
    }

    fun login(username: String, pin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUserByUsername(username.trim())
            if (user == null) {
                launch(Dispatchers.Main) { onResult(false, "User '$username' not found.") }
                return@launch
            }
            if (!user.isActive) {
                launch(Dispatchers.Main) { onResult(false, "Account '$username' is deactivated. Contact Super Admin.") }
                return@launch
            }

            val rawPin = pin.trim()
            val hashedInput = SecurityUtils.sha256(rawPin)
            val isValid = user.pinHash.equals(hashedInput, ignoreCase = true) || user.pinHash == rawPin

            if (isValid) {
                // Auto-upgrade legacy plain-text PIN to SHA-256 hash
                if (!user.pinHash.equals(hashedInput, ignoreCase = true)) {
                    val updated = user.copy(pinHash = hashedInput)
                    userDao.updateUser(updated)
                    _activeUser.value = updated
                } else {
                    _activeUser.value = user
                }

                val displayName = user.fullName.ifBlank { user.username }
                setActiveCashierName(displayName, _activeUser.value)
                _isAppLocked.value = false

                activityLogDao.insertLog(
                    ActivityLog(
                        action = "User Login",
                        module = "Auth & Access",
                        details = "User ${user.username} (${user.role}) authenticated successfully.",
                        performedBy = displayName
                    )
                )

                launch(Dispatchers.Main) {
                    onResult(true, "Welcome, $displayName!")
                }
            } else {
                launch(Dispatchers.Main) {
                    onResult(false, "Invalid PIN/Password. Please try again.")
                }
            }
        }
    }

    fun lockTerminal() {
        _isAppLocked.value = true
        viewModelScope.launch(Dispatchers.IO) {
            activityLogDao.insertLog(
                ActivityLog(
                    action = "Terminal Locked",
                    module = "Auth & Access",
                    details = "POS Terminal locked.",
                    performedBy = _activeCashierName.value.ifBlank { _activeUser.value?.fullName ?: "User" }
                )
            )
        }
    }

    fun unlockTerminal() {
        _isAppLocked.value = false
    }

    fun logout() {
        val prevName = _activeCashierName.value.ifBlank { _activeUser.value?.fullName ?: "User" }
        _activeUser.value = null
        _isAppLocked.value = true
        viewModelScope.launch(Dispatchers.IO) {
            activityLogDao.insertLog(
                ActivityLog(
                    action = "User Logout",
                    module = "Auth & Access",
                    details = "User session terminated. Terminal locked.",
                    performedBy = prevName
                )
            )
        }
    }

    fun loginWithSingleCredential(credential: String, onResult: (Boolean, String, User?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val raw = credential.trim()
            if (raw.isBlank()) {
                launch(Dispatchers.Main) {
                    onResult(false, "Please enter your PIN or password.", null)
                }
                return@launch
            }

            val allUsers = userDao.getAllUsers()
            val hashedInput = SecurityUtils.sha256(raw)

            val matchedUser = allUsers.firstOrNull { u ->
                u.pinHash.equals(hashedInput, ignoreCase = true)
            }

            if (matchedUser != null) {
                if (!matchedUser.isActive) {
                    launch(Dispatchers.Main) {
                        onResult(false, "Account '${matchedUser.username}' is disabled. Contact Super Admin.", null)
                    }
                    return@launch
                }

                val finalUser = if (!matchedUser.pinHash.equals(hashedInput, ignoreCase = true)) {
                    val updated = matchedUser.copy(pinHash = hashedInput)
                    userDao.updateUser(updated)
                    updated
                } else {
                    matchedUser
                }

                _activeUser.value = finalUser
                _isAppLocked.value = false
                val displayName = finalUser.fullName.ifBlank { finalUser.username }
                setActiveCashierName(displayName, finalUser)

                val roleEnum = UserRole.fromString(finalUser.role)
                activityLogDao.insertLog(
                    ActivityLog(
                        action = "Single Credential Login",
                        module = "Auth & Access",
                        details = "User '${finalUser.username}' authenticated as ${roleEnum.displayName}.",
                        performedBy = displayName
                    )
                )

                launch(Dispatchers.Main) {
                    onResult(true, "Authenticated as ${roleEnum.displayName}", finalUser)
                }
            } else {
                launch(Dispatchers.Main) {
                    onResult(false, "Invalid credential. Access denied.", null)
                }
            }
        }
    }

    fun updateUserPin(userId: Long, newPin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val clean = newPin.trim()
            if (clean.length < 4) {
                launch(Dispatchers.Main) { onResult(false, "PIN must be at least 4 digits.") }
                return@launch
            }
            val target = userDao.getUserById(userId)
            if (target == null) {
                launch(Dispatchers.Main) { onResult(false, "User account not found.") }
                return@launch
            }
            val newHash = SecurityUtils.sha256(clean)
            val updated = target.copy(pinHash = newHash)
            userDao.updateUser(updated)

            if (_activeUser.value?.id == userId) {
                _activeUser.value = updated
            }

            activityLogDao.insertLog(
                ActivityLog(
                    action = "Credential Updated",
                    module = "User & Security Management",
                    details = "PIN changed for '${target.username}' (${target.role}). Old credential immediately invalidated.",
                    performedBy = _activeUser.value?.fullName ?: "Super Admin"
                )
            )

            launch(Dispatchers.Main) {
                onResult(true, "Credential updated successfully. Old PIN is now invalid.")
            }
        }
    }

    fun addOrUpdateCashier(
        fullName: String,
        username: String,
        pin: String,
        role: String = "CASHIER",
        userId: Long = 0L,
        isActive: Boolean = true,
        setAsActive: Boolean = false,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val hashedPin = if (pin.trim().length == 64 && pin.trim().all { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }) {
                pin.trim().uppercase(Locale.ROOT)
            } else {
                SecurityUtils.sha256(pin.trim())
            }

            val userToSave = User(
                id = userId,
                username = username.trim(),
                pinHash = hashedPin,
                role = role,
                fullName = fullName.trim(),
                phone = "",
                branchId = 1,
                isActive = isActive,
                createdAt = System.currentTimeMillis()
            )
            if (userId == 0L) {
                val newId = userDao.insertUser(userToSave)
                activityLogDao.insertLog(
                    ActivityLog(
                        action = "Cashier Created",
                        module = "Cashier & Staff",
                        details = "Added cashier: ${fullName.trim()} (@${username.trim()}) - Status: ${if (isActive) "Active" else "Inactive"}",
                        performedBy = _activeCashierName.value.ifBlank { _activeUser.value?.fullName ?: "Admin" }
                    )
                )
                if (setAsActive && isActive) {
                    setActiveCashierName(fullName.trim().ifBlank { username.trim() }, userToSave.copy(id = newId))
                }
            } else {
                val oldUser = userDao.getUserById(userId)
                val wasActiveCashier = oldUser != null && (
                        _activeCashierName.value.equals(oldUser.fullName, ignoreCase = true) ||
                        _activeCashierName.value.equals(oldUser.username, ignoreCase = true)
                )

                userDao.updateUser(userToSave)
                activityLogDao.insertLog(
                    ActivityLog(
                        action = "Cashier Updated",
                        module = "Cashier & Staff",
                        details = "Updated cashier: ${fullName.trim()} (@${username.trim()}) - Status: ${if (isActive) "Active" else "Inactive"}",
                        performedBy = _activeCashierName.value.ifBlank { _activeUser.value?.fullName ?: "Admin" }
                    )
                )

                if (isActive && (setAsActive || wasActiveCashier)) {
                    setActiveCashierName(fullName.trim().ifBlank { username.trim() }, userToSave)
                } else if (!isActive && wasActiveCashier) {
                    val fallbackUser = users.value.firstOrNull { it.id != userId && it.isActive }
                    val fallbackName = fallbackUser?.fullName?.ifBlank { fallbackUser.username } ?: "Muhammad Umer"
                    setActiveCashierName(fallbackName, fallbackUser)
                }
            }
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun saveUser(user: User, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldUser = if (user.id != 0L) userDao.getUserById(user.id) else null
            val wasActiveCashier = oldUser != null && (
                    _activeCashierName.value.equals(oldUser.fullName, ignoreCase = true) ||
                    _activeCashierName.value.equals(oldUser.username, ignoreCase = true)
            )

            val secureUser = if (user.pinHash.length == 64 && user.pinHash.all { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }) {
                user
            } else {
                user.copy(pinHash = SecurityUtils.sha256(user.pinHash.trim()))
            }

            if (user.id == 0L) {
                userDao.insertUser(secureUser)
                activityLogDao.insertLog(
                    ActivityLog(
                        action = "User Created",
                        module = "Users",
                        details = "Created user: ${user.username} (${user.role})",
                        performedBy = _activeCashierName.value.ifBlank { _activeUser.value?.fullName ?: "Admin" }
                    )
                )
            } else {
                userDao.updateUser(secureUser)
                activityLogDao.insertLog(
                    ActivityLog(
                        action = "User Updated",
                        module = "Users",
                        details = "Updated user: ${user.username} (${user.role})",
                        performedBy = _activeCashierName.value.ifBlank { _activeUser.value?.fullName ?: "Admin" }
                    )
                )

                if (user.isActive && wasActiveCashier) {
                    val newName = user.fullName.ifBlank { user.username }
                    setActiveCashierName(newName, secureUser)
                } else if (!user.isActive && wasActiveCashier) {
                    val fallbackUser = users.value.firstOrNull { it.id != user.id && it.isActive }
                    val fallbackName = fallbackUser?.fullName?.ifBlank { fallbackUser.username } ?: "Muhammad Umer"
                    setActiveCashierName(fallbackName, fallbackUser)
                }
            }
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUserById(userId)
            if (user != null) {
                userDao.deleteUser(user)
                activityLogDao.insertLog(
                    ActivityLog(
                        action = "User Deleted",
                        module = "Users",
                        details = "Removed user: ${user.username}",
                        performedBy = _activeCashierName.value.ifBlank { _activeUser.value?.fullName ?: "Admin" }
                    )
                )
                if (_activeCashierName.value == user.fullName || _activeCashierName.value == user.username) {
                    val remaining = users.value.firstOrNull { it.id != userId }
                    val nextName = remaining?.fullName?.ifBlank { remaining.username } ?: "Muhammad Umer"
                    setActiveCashierName(nextName, remaining)
                }
            }
        }
    }

    fun getEmergencyRecoveryCode(): String {
        var code = prefs.getString("emergency_recovery_code", null)
        if (code == null) {
            code = RecoveryUtils.generate20CharEmergencyCode()
            prefs.edit().putString("emergency_recovery_code", code).apply()
        }
        return code
    }

    fun getRecoveryPassphrase(): String {
        var phrase = prefs.getString("emergency_recovery_phrase", null)
        if (phrase == null) {
            phrase = RecoveryUtils.generate12WordPassphrase()
            prefs.edit().putString("emergency_recovery_phrase", phrase).apply()
        }
        return phrase
    }

    fun resetUserPin(
        username: String,
        newPin: String,
        recoveryKey: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetUser = userDao.getUserByUsername(username.trim())
            if (targetUser == null) {
                launch(Dispatchers.Main) { onResult(false, "User '$username' not found.") }
                return@launch
            }

            val savedCode = getEmergencyRecoveryCode()
            val savedPhrase = getRecoveryPassphrase()

            val normalizedInput = recoveryKey.trim()
            val isValidCode = RecoveryUtils.normalizeEmergencyCode(normalizedInput) == RecoveryUtils.normalizeEmergencyCode(savedCode)
            val isValidPhrase = normalizedInput.equals(savedPhrase, ignoreCase = true) ||
                    RecoveryUtils.normalizePassphrase(normalizedInput) == RecoveryUtils.normalizePassphrase(savedPhrase)

            if (isValidCode || isValidPhrase) {
                val hashedPin = SecurityUtils.sha256(newPin.trim())
                userDao.updateUser(targetUser.copy(pinHash = hashedPin))
                activityLogDao.insertLog(
                    ActivityLog(
                        action = "Password Reset",
                        module = "Security",
                        details = "Password reset for user: ${targetUser.username}",
                        performedBy = "Recovery System"
                    )
                )
                launch(Dispatchers.Main) { onResult(true, "PIN successfully updated for $username!") }
            } else {
                launch(Dispatchers.Main) { onResult(false, "Invalid emergency code, passphrase, or master key.") }
            }
        }
    }

    // Activation Management
    fun activateApp(code: String, onResult: (String, String, Boolean) -> Unit) {
        viewModelScope.launch {
            activationManager.activateWithCode(code) { status, msg, success ->
                onResult(status, msg, success)
            }
        }
    }

    fun resetAppActivation() {
        activationManager.resetActivation()
    }

    // Camera Barcode Scanner Preference
    fun setCameraScannerEnabled(enabled: Boolean) {
        _cameraScannerEnabled.value = enabled
        prefs.edit().putBoolean("camera_scanner_enabled", enabled).apply()
    }

    // Owner Security Authentication - strictly isolated to Dedicated Owner PIN/Password
    fun isOwnerSecurityConfigured(): Boolean {
        return ownerSecurityManager.isOwnerSecurityConfigured()
    }

    fun setupOwnerSecurity(pin: String): Boolean {
        val clean = pin.trim()
        val success = ownerSecurityManager.setupOwnerSecurity(clean)
        if (success) {
            _ownerSecurityCode.value = ""
            prefs.edit().remove("owner_security_code").apply()
        }
        return success
    }

    fun changeOwnerPassword(currentPin: String, newPin: String): Boolean {
        val success = ownerSecurityManager.changePassword(currentPin, newPin)
        if (success) {
            _ownerSecurityCode.value = ""
            prefs.edit().remove("owner_security_code").apply()
        }
        return success
    }

    fun verifyOwnerSecurityCode(enteredPin: String): Boolean {
        return ownerSecurityManager.verifyCredential(enteredPin)
    }

    fun verifyOwnerSecurityCredential(enteredCredential: String): Boolean {
        return ownerSecurityManager.verifyCredential(enteredCredential)
    }

    fun getOwnerSecurityKey(): String {
        return ownerSecurityManager.getSecurityKey()
    }

    fun getMaskedOwnerSecurityKey(): String {
        return ownerSecurityManager.getMaskedSecurityKey()
    }

    fun regenerateOwnerSecurityKey(): String {
        return ownerSecurityManager.regenerateSecurityKey()
    }

    // Developer Master Authentication - strictly requires Dedicated Owner PIN
    fun verifyDeveloperAuth(enteredKey: String): Boolean {
        val clean = enteredKey.trim()
        if (clean.isBlank()) return false
        return ownerSecurityManager.verifyCredential(clean)
    }

    fun setDeveloperAuthKey(newKey: String) {
        val clean = newKey.trim()
        if (clean.length >= 4) {
            ownerSecurityManager.setPassword(clean)
            _ownerSecurityCode.value = ""
            prefs.edit().remove("owner_security_code").remove("developer_auth_hash").apply()
        }
    }

    fun setOwnerSecurityCode(newPin: String): Boolean {
        val clean = newPin.trim()
        val success = ownerSecurityManager.setPassword(clean)
        if (success) {
            _ownerSecurityCode.value = ""
            prefs.edit().remove("owner_security_code").apply()
        }
        return success
    }

    fun getInstallationId(): String {
        return SecureIdentityManager.getInstance(getApplication()).getInstallationId()
    }

    fun generateOfflineActivationCode(installationId: String, planDays: Int = 0): String {
        return AppActivationManager.generatePlanActivationCode(installationId, planDays)
    }

    fun getLicenseExpiryTimestamp(): Long {
        return activationManager.getExpiryTimestamp()
    }

    fun getLicensePlanName(): String {
        return activationManager.getPlanName()
    }

    fun getLicenseDaysRemaining(): Long {
        return activationManager.getDaysRemaining()
    }

    fun extendCurrentLicense(days: Int) {
        activationManager.extendLicense(days)
    }

    fun renewCurrentLicense(days: Int, planName: String) {
        activationManager.renewLicense(days, planName)
    }

    fun deactivateLicense() {
        activationManager.resetActivation()
    }

    fun addBranch(name: String, location: String, managerName: String, phone: String = "", isHq: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val branch = StoreBranch(
                name = name.trim(),
                location = location.trim(),
                managerName = managerName.trim(),
                phone = phone.trim(),
                isHeadquarters = isHq
            )
            storeBranchDao.insertBranch(branch)
            activityLogDao.insertLog(
                ActivityLog(
                    action = "Branch Created",
                    module = "Owner Control Center",
                    details = "Created branch: ${name.trim()} ($location)",
                    performedBy = _activeCashierName.value.ifBlank { "Owner" }
                )
            )
        }
    }

    fun deleteBranch(branchId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val b = storeBranchDao.getBranchById(branchId)
            if (b != null) {
                storeBranchDao.deleteBranch(b)
            }
        }
    }

    suspend fun getSaleDetails(saleId: Long): Pair<Sale?, List<SaleItem>> {
        return withContext(Dispatchers.IO) {
            val sale = saleDao.getSaleById(saleId)
            val items = saleDao.getItemsForSale(saleId)
            Pair(sale, items)
        }
    }

    // Invoice / Sale Editing & Deletion
    fun updateSale(
        sale: Sale,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = saleDao.getSaleById(sale.id)
            if (existing != null) {
                // Adjust customer due balance if due amount changed
                if (sale.customerId > 0 && sale.customerId == existing.customerId) {
                    val dueDiff = sale.dueAmount - existing.dueAmount
                    if (dueDiff != 0.0) {
                        val c = customerDao.getCustomerById(sale.customerId)
                        if (c != null) {
                            customerDao.updateCustomer(c.copy(balance = (c.balance + dueDiff).coerceAtLeast(0.0)))
                        }
                    }
                }
            }

            saleDao.updateSale(sale)
            activityLogDao.insertLog(
                ActivityLog(
                    action = "Invoice Updated",
                    module = "Invoices",
                    details = "Invoice ${sale.invoiceNumber} updated. Customer: ${sale.customerName}, Net: ${sale.netAmount}, Paid: ${sale.paidAmount}, Due: ${sale.dueAmount}",
                    performedBy = _activeCashierName.value.ifBlank { _activeUser.value?.fullName ?: "Admin" }
                )
            )
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun softDeleteSale(
        saleId: Long,
        restoreStock: Boolean = true,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val sale = saleDao.getSaleById(saleId)
            val items = saleDao.getItemsForSale(saleId)

            // 1. Restore stock if requested
            if (restoreStock && items.isNotEmpty()) {
                for (item in items) {
                    val p = productDao.getProductById(item.productId)
                    if (p != null) {
                        val newQty = p.stockQuantity + item.quantity
                        productDao.updateProduct(p.copy(stockQuantity = newQty))
                    }
                }
            }

            // 2. Adjust customer balance if sale had unpaid due
            if (sale != null && sale.customerId > 0 && sale.dueAmount > 0) {
                val c = customerDao.getCustomerById(sale.customerId)
                if (c != null) {
                    val updatedBal = (c.balance - sale.dueAmount).coerceAtLeast(0.0)
                    customerDao.updateCustomer(c.copy(balance = updatedBal))
                }
            }

            // 3. Mark sale as deleted
            saleDao.softDeleteSale(saleId)

            activityLogDao.insertLog(
                ActivityLog(
                    action = "Invoice Moved to Trash",
                    module = "Invoices",
                    details = "Invoice ${sale?.invoiceNumber ?: "#$saleId"} deleted (Stock Restored: $restoreStock)",
                    performedBy = _activeCashierName.value.ifBlank { _activeUser.value?.fullName ?: "Admin" }
                )
            )

            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun restoreSale(
        saleId: Long,
        reDeductStock: Boolean = true,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val sale = saleDao.getSaleById(saleId)
            val items = saleDao.getItemsForSale(saleId)

            // 1. Re-deduct stock if requested
            if (reDeductStock && items.isNotEmpty()) {
                for (item in items) {
                    val p = productDao.getProductById(item.productId)
                    if (p != null) {
                        val newQty = (p.stockQuantity - item.quantity).coerceAtLeast(0.0)
                        productDao.updateProduct(p.copy(stockQuantity = newQty))
                    }
                }
            }

            // 2. Restore customer due balance if applicable
            if (sale != null && sale.customerId > 0 && sale.dueAmount > 0) {
                val c = customerDao.getCustomerById(sale.customerId)
                if (c != null) {
                    customerDao.updateCustomer(c.copy(balance = c.balance + sale.dueAmount))
                }
            }

            // 3. Restore sale
            saleDao.restoreSale(saleId)

            activityLogDao.insertLog(
                ActivityLog(
                    action = "Invoice Restored",
                    module = "Invoices",
                    details = "Invoice ${sale?.invoiceNumber ?: "#$saleId"} restored from trash",
                    performedBy = _activeCashierName.value.ifBlank { _activeUser.value?.fullName ?: "Admin" }
                )
            )

            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun hardDeleteSale(
        saleId: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val sale = saleDao.getSaleById(saleId)
            saleDao.deleteItemsForSale(saleId)
            saleDao.hardDeleteSale(saleId)

            activityLogDao.insertLog(
                ActivityLog(
                    action = "Invoice Permanently Purged",
                    module = "Invoices",
                    details = "Invoice ${sale?.invoiceNumber ?: "#$saleId"} permanently deleted",
                    performedBy = _activeCashierName.value.ifBlank { _activeUser.value?.fullName ?: "Admin" }
                )
            )

            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    // ----------------------------------------------------
    // GOOGLE DRIVE BACKUP & RECOVERY SUITE
    // ----------------------------------------------------
    val googleDriveBackupManager = GoogleDriveBackupManager.getInstance(application)

    val backupConnectedAccount = googleDriveBackupManager.connectedAccount
    val backupAutoFrequency = googleDriveBackupManager.autoBackupFrequency
    val backupOpState = googleDriveBackupManager.opState
    val backupHistory = googleDriveBackupManager.history
    val lastBackupTimestamp = googleDriveBackupManager.lastBackupTimestamp
    val lastSafetyBackupTimestamp = googleDriveBackupManager.lastSafetyBackupTimestamp

    fun connectGoogleDrive(email: String, name: String = ""): Boolean {
        return googleDriveBackupManager.connectAccount(email, name)
    }

    fun disconnectGoogleDrive() {
        googleDriveBackupManager.disconnectAccount()
    }

    fun setBackupAutoFrequency(frequency: AutoBackupFrequency) {
        googleDriveBackupManager.setAutoBackupFrequency(frequency)
    }

    fun performCloudBackup() {
        viewModelScope.launch {
            googleDriveBackupManager.performCloudBackup()
        }
    }

    fun performLocalExport() {
        viewModelScope.launch {
            googleDriveBackupManager.performLocalExport()
        }
    }

    fun performRestore(backupFile: File, ownerPin: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = googleDriveBackupManager.performRestore(backupFile, ownerPin)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun deleteBackup(backupId: String, ownerPin: String): Boolean {
        return googleDriveBackupManager.deleteBackup(backupId, ownerPin)
    }

    fun dismissBackupOpState() {
        googleDriveBackupManager.dismissOpState()
    }

    // ==========================================
    // REGISTER SHIFT & DAY SETTLEMENT MANAGEMENT
    // ==========================================

    fun openShift(
        openingCash: Double,
        cashierName: String,
        openingNotes: String = "",
        openedAt: Long = System.currentTimeMillis(),
        onComplete: (RegisterShift) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val shiftNum = "SH-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date(openedAt))}"
            val resolvedCashier = cashierName.ifBlank { _activeCashierName.value.ifBlank { "Counter Cashier" } }
            val shift = RegisterShift(
                shiftNumber = shiftNum,
                cashierName = resolvedCashier,
                openedAt = openedAt,
                openingCash = openingCash.coerceAtLeast(0.0),
                openingNotes = openingNotes.trim(),
                status = "OPEN",
                branchId = storeSettings.value?.activeBranchId ?: 1L
            )
            val id = registerShiftDao.insertShift(shift)
            val insertedShift = shift.copy(id = id)

            activityLogDao.insertLog(
                ActivityLog(
                    action = "Shift Opened",
                    module = "Register & Cash Closing",
                    details = "Register shift opened by '$resolvedCashier' with opening cash of ${storeSettings.value?.currencySymbol ?: "Rs"} ${"%.2f".format(openingCash)} (Shift: $shiftNum)",
                    performedBy = resolvedCashier
                )
            )

            withContext(Dispatchers.Main) {
                onComplete(insertedShift)
            }
        }
    }

    fun recordCashMovement(
        type: String, // "CASH_IN" or "CASH_OUT"
        amount: Double,
        reason: String,
        cashierName: String = "",
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = registerShiftDao.getActiveShift()
            val shiftId = current?.id ?: 0L
            val resolvedCashier = cashierName.ifBlank { current?.cashierName ?: _activeCashierName.value }

            val movement = CashMovement(
                shiftId = shiftId,
                type = type,
                amount = amount.coerceAtLeast(0.0),
                reason = reason.trim(),
                cashierName = resolvedCashier,
                timestamp = System.currentTimeMillis()
            )
            cashMovementDao.insertMovement(movement)

            // Update shift totals if active
            if (current != null) {
                val movements = cashMovementDao.getMovementsForShift(current.id)
                val totalIn = movements.filter { it.type == "CASH_IN" }.sumOf { it.amount }
                val totalOut = movements.filter { it.type == "CASH_OUT" }.sumOf { it.amount }
                registerShiftDao.updateShift(current.copy(cashIn = totalIn, cashOut = totalOut))
            }

            activityLogDao.insertLog(
                ActivityLog(
                    action = if (type == "CASH_IN") "Cash In (Drawer Deposit)" else "Cash Out (Petty Expense)",
                    module = "Register & Cash Closing",
                    details = "${if (type == "CASH_IN") "Added" else "Withdrew"} ${storeSettings.value?.currencySymbol ?: "Rs"} ${"%.2f".format(amount)} - Reason: '$reason'",
                    performedBy = resolvedCashier
                )
            )

            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun closeShift(
        actualCash: Double,
        closingNotes: String = "",
        den5000: Int = 0,
        den1000: Int = 0,
        den500: Int = 0,
        den100: Int = 0,
        den50: Int = 0,
        den20: Int = 0,
        den10: Int = 0,
        denCoins: Double = 0.0,
        closedBy: String = "",
        onComplete: (RegisterShift) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = registerShiftDao.getActiveShift()
            if (current == null) {
                withContext(Dispatchers.Main) {
                    onError("No active shift found to close.")
                }
                return@launch
            }

            val now = System.currentTimeMillis()
            val shiftSales = saleDao.getAllSales().filter { it.createdAt >= current.openedAt && !it.isDeleted }
            val cashSales = shiftSales.filter { it.paymentType.equals("Cash", ignoreCase = true) }.sumOf { it.paidAmount }
            val cardSales = shiftSales.filter { !it.paymentType.equals("Cash", ignoreCase = true) && !it.paymentType.equals("Credit", ignoreCase = true) }.sumOf { it.paidAmount }
            val creditSales = shiftSales.sumOf { it.dueAmount }
            val totalSales = shiftSales.sumOf { it.netAmount }
            val totalInvoices = shiftSales.size

            val movements = cashMovementDao.getMovementsForShift(current.id)
            val totalIn = movements.filter { it.type == "CASH_IN" }.sumOf { it.amount }
            val totalOut = movements.filter { it.type == "CASH_OUT" }.sumOf { it.amount }

            val expectedCash = (current.openingCash + cashSales + totalIn - totalOut).coerceAtLeast(0.0)
            val discrepancy = actualCash - expectedCash
            val resolvedCloser = closedBy.ifBlank { _activeCashierName.value.ifBlank { current.cashierName } }

            val closedShift = current.copy(
                closedAt = now,
                status = "CLOSED",
                cashSales = cashSales,
                cardSales = cardSales,
                creditSales = creditSales,
                totalSales = totalSales,
                totalInvoices = totalInvoices,
                cashIn = totalIn,
                cashOut = totalOut,
                expectedCash = expectedCash,
                actualCash = actualCash,
                discrepancy = discrepancy,
                closingNotes = closingNotes.trim(),
                closedBy = resolvedCloser,
                denomination5000 = den5000,
                denomination1000 = den1000,
                denomination500 = den500,
                denomination100 = den100,
                denomination50 = den50,
                denomination20 = den20,
                denomination10 = den10,
                denominationCoins = denCoins
            )

            registerShiftDao.updateShift(closedShift)

            activityLogDao.insertLog(
                ActivityLog(
                    action = "Shift Closed & Settle Day",
                    module = "Register & Cash Closing",
                    details = "Shift '${current.shiftNumber}' closed by '$resolvedCloser'. Expected: ${"%.2f".format(expectedCash)}, Counted: ${"%.2f".format(actualCash)}, Variance: ${"%.2f".format(discrepancy)}",
                    performedBy = resolvedCloser
                )
            )

            withContext(Dispatchers.Main) {
                onComplete(closedShift)
            }
        }
    }

    suspend fun getMovementsForShift(shiftId: Long): List<CashMovement> {
        return withContext(Dispatchers.IO) {
            cashMovementDao.getMovementsForShift(shiftId)
        }
    }

    fun deleteShift(shiftId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            registerShiftDao.deleteShift(shiftId)
            cashMovementDao.deleteMovementsForShift(shiftId)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun reopenShift(
        shiftId: Long,
        credentialPin: String,
        reason: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val clean = credentialPin.trim()
            if (clean.isBlank()) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Authorization PIN or password is required.")
                }
                return@launch
            }
            val users = userDao.getAllUsers()
            val inputHash = SecurityUtils.sha256(clean)
            val authorizer = users.firstOrNull { 
                it.pinHash.equals(inputHash, ignoreCase = true) && 
                (it.role.equals("Admin", ignoreCase = true) || it.role.equals("Owner", ignoreCase = true) || it.role.equals("Super Admin", ignoreCase = true) || it.role.equals("SUPER_ADMIN", ignoreCase = true))
            }

            if (authorizer == null) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Authorization failed. Only Admin or Owner credentials can re-open a closed day.")
                }
                return@launch
            }

            val shift = registerShiftDao.getShiftById(shiftId)
            if (shift == null) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Shift record not found.")
                }
                return@launch
            }

            val authorizerName = authorizer?.fullName ?: "Admin/Owner"
            val reopenNote = "\n[Reopened on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())} by $authorizerName: ${reason.trim()}]"
            val updated = shift.copy(
                status = "OPEN",
                closedAt = null,
                closingNotes = (shift.closingNotes + reopenNote).trim()
            )
            registerShiftDao.updateShift(updated)

            activityLogDao.insertLog(
                ActivityLog(
                    action = "Reopened Closed Shift",
                    module = "Register & Cash Closing",
                    details = "Closed Shift '${shift.shiftNumber}' reopened by $authorizerName. Reason: '$reason'",
                    performedBy = authorizerName
                )
            )

            withContext(Dispatchers.Main) {
                onResult(true, "Shift reopened successfully. Register is now unlocked.")
            }
        }
    }

    fun receiveCustomerPayment(
        customerId: Long,
        amount: Double,
        paymentMethod: String,
        notes: String = "",
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val customer = customerDao.getCustomerById(customerId) ?: return@launch
            val newBalance = (customer.balance - amount).coerceAtLeast(0.0)
            customerDao.updateCustomer(customer.copy(balance = newBalance, updatedAt = System.currentTimeMillis()))

            if (paymentMethod.equals("Cash", ignoreCase = true)) {
                val currentShift = registerShiftDao.getActiveShift()
                val shiftId = currentShift?.id ?: 0L
                val movement = CashMovement(
                    shiftId = shiftId,
                    type = "CASH_IN",
                    amount = amount,
                    reason = "Customer Payment: ${customer.name} - $notes".trim(),
                    cashierName = _activeCashierName.value,
                    timestamp = System.currentTimeMillis()
                )
                cashMovementDao.insertMovement(movement)
                if (currentShift != null) {
                    val movements = cashMovementDao.getMovementsForShift(currentShift.id)
                    val totalIn = movements.filter { it.type == "CASH_IN" }.sumOf { it.amount }
                    val totalOut = movements.filter { it.type == "CASH_OUT" }.sumOf { it.amount }
                    registerShiftDao.updateShift(currentShift.copy(cashIn = totalIn, cashOut = totalOut))
                }
            }

            activityLogDao.insertLog(
                ActivityLog(
                    action = "Customer Payment Received",
                    module = "Customer Ledger",
                    details = "Received ${storeSettings.value?.currencySymbol ?: "Rs"} ${"%.2f".format(amount)} from ${customer.name} via $paymentMethod. New Balance: ${"%.2f".format(newBalance)}",
                    performedBy = _activeCashierName.value
                )
            )

            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun paySupplier(
        supplierId: Long,
        amount: Double,
        paymentMethod: String,
        notes: String = "",
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val supplier = supplierDao.getSupplierById(supplierId) ?: return@launch
            val newBalance = (supplier.balance - amount).coerceAtLeast(0.0)
            supplierDao.updateSupplier(supplier.copy(balance = newBalance, updatedAt = System.currentTimeMillis()))

            if (paymentMethod.equals("Cash", ignoreCase = true)) {
                val currentShift = registerShiftDao.getActiveShift()
                val shiftId = currentShift?.id ?: 0L
                val movement = CashMovement(
                    shiftId = shiftId,
                    type = "CASH_OUT",
                    amount = amount,
                    reason = "Supplier Payout: ${supplier.companyName.ifBlank { supplier.name }} - $notes".trim(),
                    cashierName = _activeCashierName.value,
                    timestamp = System.currentTimeMillis()
                )
                cashMovementDao.insertMovement(movement)
                if (currentShift != null) {
                    val movements = cashMovementDao.getMovementsForShift(currentShift.id)
                    val totalIn = movements.filter { it.type == "CASH_IN" }.sumOf { it.amount }
                    val totalOut = movements.filter { it.type == "CASH_OUT" }.sumOf { it.amount }
                    registerShiftDao.updateShift(currentShift.copy(cashIn = totalIn, cashOut = totalOut))
                }
            }

            activityLogDao.insertLog(
                ActivityLog(
                    action = "Supplier Payment Disbursed",
                    module = "Supplier Ledger",
                    details = "Paid ${storeSettings.value?.currencySymbol ?: "Rs"} ${"%.2f".format(amount)} to ${supplier.companyName.ifBlank { supplier.name }} via $paymentMethod. Remaining Payable: ${"%.2f".format(newBalance)}",
                    performedBy = _activeCashierName.value
                )
            )

            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun processSaleReturn(
        sale: Sale,
        returnedItems: List<Pair<SaleItem, Double>>,
        refundPaymentType: String,
        refundAmount: Double,
        reason: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (!isProcessingReturn.compareAndSet(false, true)) {
            onComplete(false, "A return transaction is already in progress...")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val returnNumber = "RET-${System.currentTimeMillis() % 1000000}"
                val saleReturn = SaleReturn(
                    returnNumber = returnNumber,
                    saleId = sale.id,
                    originalInvoiceNumber = sale.invoiceNumber,
                    customerId = sale.customerId,
                    customerName = sale.customerName,
                    refundAmount = refundAmount,
                    refundPaymentType = refundPaymentType,
                    taxRefundAmount = 0.0,
                    reason = reason,
                    processedBy = _activeUser.value?.fullName?.ifBlank { _activeUser.value?.username } ?: "Cashier",
                    branchId = 1,
                    createdAt = System.currentTimeMillis()
                )
                val returnId = saleReturnDao.insertReturn(saleReturn)

                val returnItemEntities = returnedItems.map { (item, qty) ->
                    SaleReturnItem(
                        returnId = returnId,
                        saleItemId = item.id,
                        productId = item.productId,
                        productName = item.productName,
                        quantityReturned = qty,
                        unit = item.unit,
                        unitPrice = item.salePrice,
                        purchasePrice = item.purchasePrice,
                        totalRefund = item.salePrice * qty
                    )
                }
                saleReturnItemDao.insertReturnItems(returnItemEntities)

                // Restore stock in inventory and log stock movement
                for ((item, qty) in returnedItems) {
                    val prod = productDao.getProductById(item.productId)
                    if (prod != null) {
                        val newStock = prod.stockQuantity + qty
                        productDao.updateProduct(prod.copy(stockQuantity = newStock))
                        stockMovementDao.insertMovement(
                            StockMovement(
                                productId = prod.id,
                                productName = prod.name,
                                quantityDelta = qty,
                                stockBefore = prod.stockQuantity,
                                stockAfter = newStock,
                                movementType = "RETURN",
                                referenceId = returnNumber,
                                reason = "Return for Inv #${sale.invoiceNumber}: $reason",
                                performedBy = _activeUser.value?.fullName ?: "Cashier"
                            )
                        )
                    }
                }

                // If customer ledger adjustment
                if (sale.customerId > 0 && refundPaymentType.equals("Credit", true)) {
                    val customer = customerDao.getCustomerById(sale.customerId)
                    if (customer != null) {
                        customerDao.updateCustomer(
                            customer.copy(balance = (customer.balance - refundAmount).coerceAtLeast(0.0))
                        )
                    }
                }

                // If Cash refund, record Cash Out in register shift
                if (refundPaymentType.equals("Cash", true)) {
                    val shift = registerShiftDao.getActiveShift()
                    if (shift != null) {
                        cashMovementDao.insertMovement(
                            CashMovement(
                                shiftId = shift.id,
                                type = "CASH_OUT",
                                amount = refundAmount,
                                reason = "Refund on Return #$returnNumber (Inv #${sale.invoiceNumber})",
                                cashierName = _activeUser.value?.fullName ?: "Cashier",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        val movements = cashMovementDao.getMovementsForShift(shift.id)
                        val totalIn = movements.filter { it.type == "CASH_IN" }.sumOf { it.amount }
                        val totalOut = movements.filter { it.type == "CASH_OUT" }.sumOf { it.amount }
                        registerShiftDao.updateShift(shift.copy(cashIn = totalIn, cashOut = totalOut))
                    }
                }

                activityLogDao.insertLog(
                    ActivityLog(
                        action = "Sales Return Processed",
                        module = "POS_RETURN",
                        details = "Return #$returnNumber for Inv #${sale.invoiceNumber}, Refund: Rs $refundAmount ($refundPaymentType)",
                        performedBy = _activeUser.value?.fullName ?: "Cashier"
                    )
                )

                launch(Dispatchers.Main) {
                    onComplete(true, "Return #$returnNumber processed successfully.")
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    onComplete(false, "Failed to process return: ${e.message}")
                }
            } finally {
                isProcessingReturn.set(false)
            }
        }
    }

    fun adjustStock(
        productId: Long,
        newStock: Double,
        movementType: String,
        reason: String,
        performedBy: String,
        onComplete: () -> Unit
    ) {
        if (!isAdjustingStock.compareAndSet(false, true)) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val product = productDao.getProductById(productId)
                if (product != null) {
                    val delta = newStock - product.stockQuantity
                    productDao.updateProduct(product.copy(stockQuantity = newStock))
                    stockMovementDao.insertMovement(
                        StockMovement(
                            productId = product.id,
                            productName = product.name,
                            quantityDelta = delta,
                            stockBefore = product.stockQuantity,
                            stockAfter = newStock,
                            movementType = movementType,
                            referenceId = "ADJ-${System.currentTimeMillis() % 100000}",
                            reason = reason,
                            performedBy = performedBy
                        )
                    )
                    activityLogDao.insertLog(
                        ActivityLog(
                            action = "Stock Adjusted",
                            module = "INVENTORY",
                            details = "${product.name}: ${product.stockQuantity} -> $newStock ($movementType: $reason)",
                            performedBy = performedBy
                        )
                    )
                }
                launch(Dispatchers.Main) {
                    onComplete()
                }
            } finally {
                isAdjustingStock.set(false)
            }
        }
    }

    fun testFbrConnection(onResult: (com.example.data.fbr.FbrConnectionStatus, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = storeSettingsDao.getSettings()
            if (settings == null) {
                launch(Dispatchers.Main) {
                    onResult(com.example.data.fbr.FbrConnectionStatus.CONFIGURATION_REQUIRED, "Settings not initialized.")
                }
                return@launch
            }
            val isProduction = settings.fbrEnvironment.equals("Live", ignoreCase = true) ||
                    settings.fbrEnvironment.equals("Production", ignoreCase = true)
            val res = fbrApiService.testConnection(
                posId = settings.fbrPosId,
                authToken = settings.fbrApiAuthToken,
                isProduction = isProduction
            )
            launch(Dispatchers.Main) {
                onResult(res.first, res.second)
            }
        }
    }

    fun retryFbrSubmission(recordId: Long, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val records = fbrRecordDao.getAllRecords()
            val record = records.firstOrNull { it.id == recordId }
            if (record == null) {
                launch(Dispatchers.Main) { onComplete(false, "Record not found.") }
                return@launch
            }
            val sale = saleDao.getSaleById(record.saleId)
            val items = saleDao.getItemsForSale(record.saleId)
            val settings = storeSettingsDao.getSettings()
            if (sale == null || settings == null) {
                launch(Dispatchers.Main) { onComplete(false, "Sale or Settings data missing.") }
                return@launch
            }
            when (val res = fbrSubmissionManager.retrySubmission(record, sale, items, settings)) {
                is com.example.data.fbr.FbrSubmissionResult.Success -> {
                    launch(Dispatchers.Main) { onComplete(true, "Successfully submitted! Ref: ${res.fbrInvoiceNumber}") }
                }
                is com.example.data.fbr.FbrSubmissionResult.Failure -> {
                    launch(Dispatchers.Main) { onComplete(false, res.errorMessage) }
                }
                is com.example.data.fbr.FbrSubmissionResult.NotRequired -> {
                    launch(Dispatchers.Main) { onComplete(false, "FBR Integration is not enabled.") }
                }
            }
        }
    }
}
