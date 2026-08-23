package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.model.ApiResult
import com.example.data.api.network.ConnectionState
import com.example.data.api.network.NetworkConnectionMonitor
import com.example.data.api.repository.DeveloperApiRepository
import com.example.data.api.security.AppActivationManager
import com.example.data.api.security.SecureIdentityManager
import com.example.data.db.AppDatabase
import com.example.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CartItem(
    val product: Product,
    var quantity: Double = 1.0,
    var unitPrice: Double = product.salePrice
) {
    val totalPrice: Double get() = quantity * unitPrice
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
    private val businessProfileDao = db.businessProfileDao()
    private val activityLogDao = db.activityLogDao()

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

    val activityLogs: StateFlow<List<ActivityLog>> = activityLogDao.getRecentLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recycleBinProducts: StateFlow<List<Product>> = productDao.getRecycleBinProductsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recycleBinCustomers: StateFlow<List<Customer>> = customerDao.getRecycleBinCustomersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recycleBinSales: StateFlow<List<Sale>> = saleDao.getRecycleBinSalesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _lastCompletedSale = MutableStateFlow<Sale?>(null)
    val lastCompletedSale: StateFlow<Sale?> = _lastCompletedSale.asStateFlow()

    private val _lastCompletedSaleItems = MutableStateFlow<List<SaleItem>>(emptyList())
    val lastCompletedSaleItems: StateFlow<List<SaleItem>> = _lastCompletedSaleItems.asStateFlow()

    init {
        // Initial setup
        viewModelScope.launch(Dispatchers.IO) {
            val user = userDao.getUserByUsername("admin")
            if (user != null) {
                _activeUser.value = user
            }
        }
    }

    // POS Cart Operations
    fun addToCart(product: Product, quantity: Double = 1.0) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = currentList[index]
            currentList[index] = item.copy(quantity = item.quantity + quantity)
        } else {
            currentList.add(CartItem(product = product, quantity = quantity, unitPrice = product.salePrice))
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

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filterNot { it.product.id == productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _discountAmount.value = 0.0
        _receivedAmount.value = 0.0
        _selectedCustomer.value = null
    }

    fun setSelectedCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
    }

    fun setDiscount(discount: Double) {
        _discountAmount.value = discount.coerceAtLeast(0.0)
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

    fun completeSale(
        onSuccess: (Sale, List<SaleItem>) -> Unit = { _, _ -> },
        onError: (String) -> Unit = {}
    ) {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) {
            onError("Cart is empty.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val subtotal = currentCart.sumOf { it.totalPrice }
            val discount = _discountAmount.value
            val taxAmount = ((subtotal - discount).coerceAtLeast(0.0) * _taxRatePercent.value) / 100.0
            val netAmount = (subtotal - discount + taxAmount).coerceAtLeast(0.0)
            val paid = if (_receivedAmount.value > 0) _receivedAmount.value else netAmount
            val due = (netAmount - paid).coerceAtLeast(0.0)

            val invoiceNo = "INV-${System.currentTimeMillis() % 1000000}"
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
                cashierName = _activeUser.value?.fullName ?: "Cashier",
                branchId = 1,
                createdAt = System.currentTimeMillis()
            )

            val saleId = saleDao.insertSale(sale)

            val saleItems = currentCart.map {
                SaleItem(
                    saleId = saleId,
                    productId = it.product.id,
                    productName = it.product.name,
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
                    details = "Invoice $invoiceNo: Net $netAmount, Paid $paid",
                    performedBy = _activeUser.value?.fullName ?: "Cashier"
                )
            )

            val completedSale = sale.copy(id = saleId)
            _lastCompletedSale.value = completedSale
            _lastCompletedSaleItems.value = saleItems

            launch(Dispatchers.Main) {
                clearCart()
                onSuccess(completedSale, saleItems)
            }
        }
    }

    // Product Management
    fun saveProduct(product: Product, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            if (product.id == 0L) {
                productDao.insertProduct(product)
                activityLogDao.insertLog(
                    ActivityLog(
                        action = "Product Created",
                        module = "Inventory",
                        details = "Added product: ${product.name}",
                        performedBy = _activeUser.value?.fullName ?: "Admin"
                    )
                )
            } else {
                productDao.updateProduct(product)
                activityLogDao.insertLog(
                    ActivityLog(
                        action = "Product Updated",
                        module = "Inventory",
                        details = "Updated product: ${product.name}",
                        performedBy = _activeUser.value?.fullName ?: "Admin"
                    )
                )
            }
            launch(Dispatchers.Main) { onSuccess() }
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

            // Update stock for purchased products
            for (item in items) {
                val p = productDao.getProductById(item.productId)
                if (p != null) {
                    val newStock = p.stockQuantity + item.quantity
                    productDao.updateProduct(p.copy(stockQuantity = newStock, purchasePrice = item.unitCost))
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

    fun updateBusinessProfile(profile: BusinessProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            businessProfileDao.insertOrUpdateProfile(profile)
        }
    }

    // Attendance Management
    fun recordAttendance(
        employeeName: String,
        status: String = "Present",
        notes: String = "",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(Date())
            val record = AttendanceRecord(
                employeeName = employeeName,
                dateString = today,
                checkInTime = System.currentTimeMillis(),
                status = status,
                notes = notes
            )
            attendanceDao.insertAttendance(record)
            launch(Dispatchers.Main) { onSuccess() }
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

    suspend fun getSaleDetails(saleId: Long): Pair<Sale?, List<SaleItem>> {
        return withContext(Dispatchers.IO) {
            val sale = saleDao.getSaleById(saleId)
            val items = saleDao.getItemsForSale(saleId)
            Pair(sale, items)
        }
    }
}
