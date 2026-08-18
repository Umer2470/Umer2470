package com.example.data.repository

import com.example.data.dao.StoreDao
import com.example.data.entity.CategoryEntity
import com.example.data.entity.Customer
import com.example.data.entity.CustomerLedger
import com.example.data.entity.Expense
import com.example.data.entity.Product
import com.example.data.entity.Purchase
import com.example.data.entity.PurchaseItem
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreProfile
import com.example.data.entity.StoreSettings
import com.example.data.entity.Supplier
import com.example.data.entity.SupplierLedger
import com.example.data.entity.UserAccount
import com.example.data.entity.DailyClosing
import com.example.data.entity.ActivityLog
import com.example.data.entity.UserStorePermission
import com.example.data.entity.RecycleBinItem
import com.example.data.entity.SuperAdminRecovery
import com.example.data.entity.AttendanceRecord
import com.example.util.BatchImportResult
import com.example.util.BatchProductImportItem
import com.example.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

import com.example.data.entity.TenantAccount
import com.example.data.entity.SubscriptionPlan
import com.example.util.LicenseManager

import android.content.Context
import com.example.data.api.repository.DeveloperApiRepository

class StoreRepository(
    private val dao: StoreDao,
    context: Context? = null
) {
    val developerApiRepository: DeveloperApiRepository? = context?.let { DeveloperApiRepository(it) }


    // --- MULTI-TENANT SAAS MANAGEMENT ---
    val allTenants: Flow<List<TenantAccount>> = dao.getAllTenants()
    val allSubscriptionPlans: Flow<List<SubscriptionPlan>> = dao.getAllSubscriptionPlans()

    suspend fun getTenantById(id: String): TenantAccount? {
        return withContext(Dispatchers.IO) {
            dao.getTenantById(id)
        }
    }

    suspend fun getTenantByCode(code: String): TenantAccount? {
        return withContext(Dispatchers.IO) {
            dao.getTenantByCode(code)
        }
    }

    suspend fun getTenantByLicenseKey(key: String): TenantAccount? {
        return withContext(Dispatchers.IO) {
            dao.getTenantByLicenseKey(key)
        }
    }

    suspend fun saveTenant(tenant: TenantAccount) {
        withContext(Dispatchers.IO) {
            dao.insertTenant(tenant)
        }
    }

    suspend fun deleteTenant(tenantId: String) {
        withContext(Dispatchers.IO) {
            dao.deleteTenantById(tenantId)
            dao.clearStoresForTenant(tenantId)
            dao.clearProductsForTenant(tenantId)
            dao.clearUsersForTenant(tenantId)
        }
    }

    suspend fun ensureMasterAndDefaultTenantsExist() {
        withContext(Dispatchers.IO) {
            // Seed plans
            LicenseManager.DEFAULT_PLANS.forEach { plan ->
                dao.insertSubscriptionPlan(plan)
            }

            // Ensure Master Owner Account
            var master = dao.getTenantById("TENANT_MASTER_OWNER")
            if (master == null) {
                master = LicenseManager.createDefaultMasterAccount()
                dao.insertTenant(master)
            }

            // Ensure Default Customer Tenant
            var defaultTenant = dao.getTenantById("TENANT_DEFAULT")
            if (defaultTenant == null) {
                defaultTenant = LicenseManager.createInitialCustomerTenant("TENANT_DEFAULT", "AL-KHAIR-POS")
                dao.insertTenant(defaultTenant)
            }
        }
    }

    suspend fun createCustomerTenant(
        tenantCode: String,
        businessName: String,
        ownerName: String,
        email: String,
        phone: String,
        planType: String,
        maxShops: Int = 10,
        maxUsers: Int = 25,
        customExpiryDays: Int? = null
    ): TenantAccount {
        return withContext(Dispatchers.IO) {
            val cleanCode = tenantCode.uppercase().trim().replace(" ", "_")
            val id = "TENANT_${System.currentTimeMillis()}"
            val licenseKey = LicenseManager.generateLicenseKey(cleanCode, planType)
            val plan = LicenseManager.DEFAULT_PLANS.find { it.id == planType } ?: LicenseManager.DEFAULT_PLANS[2]
            val durationDays = customExpiryDays ?: plan.durationDays
            val expiryMs = System.currentTimeMillis() + (durationDays * 24L * 60 * 60 * 1000)

            val tenant = TenantAccount(
                id = id,
                tenantCode = cleanCode,
                businessName = businessName,
                ownerName = ownerName,
                email = email,
                phone = phone,
                licenseKey = licenseKey,
                planType = plan.id,
                status = "ACTIVE",
                maxShops = maxShops,
                maxUsers = maxUsers,
                licenseExpiryDate = expiryMs,
                masterSecretCode = LicenseManager.generateMasterSecretCode(cleanCode)
            )

            dao.insertTenant(tenant)

            // Provision Initial Primary Store for this Customer
            val primaryStore = StoreProfile(
                tenantId = tenant.id,
                storeName = "$businessName (Main Branch)",
                ownerName = ownerName,
                phone = phone,
                code = "MAIN",
                isPrimary = true,
                secretCode = tenant.masterSecretCode
            )
            val storeId = dao.insertStore(primaryStore)

            // Provision Initial Super Admin for this Customer Tenant
            val superAdmin = UserAccount(
                tenantId = tenant.id,
                username = "admin_${cleanCode.lowercase()}",
                name = "$ownerName (Super Admin)",
                pinCode = "1234",
                role = "SUPER_ADMIN",
                assignedStoreId = storeId,
                assignedStoreIdsCsv = "$storeId"
            )
            dao.insertUser(superAdmin)

            // Provision Initial Settings
            val settings = StoreSettings(
                tenantId = tenant.id,
                storeName = businessName,
                ownerName = ownerName,
                phone = phone,
                activeStoreId = storeId
            )
            dao.saveSettings(settings)

            tenant
        }
    }

    val settings: Flow<StoreSettings?> = dao.getSettings()
    val allStores: Flow<List<StoreProfile>> = dao.getAllStores()
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val allProducts: Flow<List<Product>> = dao.getAllProducts()
    val lowStockProducts: Flow<List<Product>> = dao.getLowStockProducts()

    suspend fun getStoreById(id: Long): StoreProfile? {
        return withContext(Dispatchers.IO) {
            dao.getStoreById(id)
        }
    }

    suspend fun saveStore(store: StoreProfile): Long {
        return withContext(Dispatchers.IO) {
            if (store.id == 0L) {
                dao.insertStore(store)
            } else {
                dao.updateStore(store)
                store.id
            }
        }
    }

    suspend fun deleteStore(store: StoreProfile) {
        withContext(Dispatchers.IO) {
            dao.deleteStore(store)
        }
    }

    fun getLowStockProductsWithThreshold(threshold: Double): Flow<List<Product>> =
        dao.getLowStockProductsWithThreshold(threshold)

    suspend fun updateMinStockThresholds(threshold: Double) {
        withContext(Dispatchers.IO) {
            dao.updateMinStockThresholds(threshold)
        }
    }
    val allCustomers: Flow<List<Customer>> = dao.getAllCustomers()
    val allSuppliers: Flow<List<Supplier>> = dao.getAllSuppliers()
    val allSales: Flow<List<Sale>> = dao.getAllSales()
    val allSaleItems: Flow<List<SaleItem>> = dao.getAllSaleItems()
    val allPurchases: Flow<List<Purchase>> = dao.getAllPurchases()
    val allExpenses: Flow<List<Expense>> = dao.getAllExpenses()

    // --- APP USERS, DAILY CLOSINGS & ACTIVITY LOGS ---
    val allUsers: Flow<List<UserAccount>> = dao.getAllUsers()
    val allDailyClosings: Flow<List<DailyClosing>> = dao.getAllDailyClosings()
    val allActivityLogs: Flow<List<ActivityLog>> = dao.getAllActivityLogs()
    val allUserStorePermissions: Flow<List<UserStorePermission>> = dao.getAllUserStorePermissions()

    // --- SUPER ADMIN RECOVERY SYSTEM ---
    val superAdminRecovery: Flow<SuperAdminRecovery?> = dao.getSuperAdminRecoveryFlow()

    suspend fun getSuperAdminRecoverySync(): SuperAdminRecovery? {
        return withContext(Dispatchers.IO) {
            dao.getSuperAdminRecoverySync()
        }
    }

    suspend fun saveSuperAdminRecovery(recovery: SuperAdminRecovery) {
        withContext(Dispatchers.IO) {
            dao.insertSuperAdminRecovery(recovery)
        }
    }

    fun getUserPermissions(userId: Long): Flow<List<UserStorePermission>> = dao.getUserPermissions(userId)
    fun getPermissionsForStore(storeId: Long): Flow<List<UserStorePermission>> = dao.getPermissionsForStore(storeId)

    suspend fun getUserPermissionForStore(userId: Long, storeId: Long): UserStorePermission? {
        return withContext(Dispatchers.IO) {
            dao.getUserPermissionForStore(userId, storeId)
        }
    }

    suspend fun saveUserPermission(permission: UserStorePermission): Long {
        return withContext(Dispatchers.IO) {
            dao.saveUserPermission(permission)
        }
    }

    suspend fun revokeUserStorePermission(userId: Long, storeId: Long) {
        withContext(Dispatchers.IO) {
            dao.revokeUserStorePermission(userId, storeId)
        }
    }

    fun getDailyClosingsForUser(userId: Long): Flow<List<DailyClosing>> = dao.getDailyClosingsByUserId(userId)

    suspend fun getUserByPin(pin: String): UserAccount? {
        return withContext(Dispatchers.IO) {
            dao.getUserByPin(pin)
        }
    }

    suspend fun getUserById(id: Long): UserAccount? {
        return withContext(Dispatchers.IO) {
            dao.getUserById(id)
        }
    }

    suspend fun saveUser(user: UserAccount): Long {
        return withContext(Dispatchers.IO) {
            if (user.id == 0L) {
                dao.insertUser(user)
            } else {
                dao.updateUser(user)
                user.id
            }
        }
    }

    suspend fun deleteUser(user: UserAccount) {
        withContext(Dispatchers.IO) {
            dao.deleteUser(user)
        }
    }

    suspend fun logActivity(userId: Long, userName: String, userRole: String, action: String, details: String = "") {
        withContext(Dispatchers.IO) {
            dao.insertActivityLog(
                ActivityLog(
                    userId = userId,
                    userName = userName,
                    userRole = userRole,
                    action = action,
                    details = details
                )
            )
        }
    }

    suspend fun getOpenDailyClosingForUser(userId: Long): DailyClosing? {
        return withContext(Dispatchers.IO) {
            dao.getOpenDailyClosingForUser(userId)
        }
    }

    suspend fun saveDailyClosing(closing: DailyClosing): Long {
        return withContext(Dispatchers.IO) {
            if (closing.id == 0L) {
                dao.insertDailyClosing(closing)
            } else {
                dao.updateDailyClosing(closing)
                closing.id
            }
        }
    }

    fun getExpensesByDateRange(startTime: Long, endTime: Long): Flow<List<Expense>> =
        dao.getExpensesByDateRange(startTime, endTime)

    suspend fun addExpense(expense: Expense): Long {
        return withContext(Dispatchers.IO) {
            dao.insertExpense(expense)
        }
    }

    suspend fun deleteExpense(expense: Expense) {
        withContext(Dispatchers.IO) {
            dao.deleteExpense(expense)
        }
    }

    fun searchProducts(query: String): Flow<List<Product>> = dao.searchProducts(query)

    suspend fun getSettingsSync(): StoreSettings {
        return withContext(Dispatchers.IO) {
            val s = dao.getSettingsSync()
            if (s == null) {
                val newSettings = StoreSettings(
                    storeName = "",
                    ownerName = "",
                    phone = "",
                    whatsappNumber = "",
                    address = "",
                    businessType = "",
                    currencySymbol = "Rs.",
                    isOnboardingCompleted = false
                )
                dao.saveSettings(newSettings)
                newSettings
            } else if (s.storeName.contains("CH UMAIR") || s.storeName.contains("یا اللہ") || s.storeName.contains("الخیر")) {
                val updated = s.copy(
                    storeName = "",
                    ownerName = "",
                    phone = "",
                    whatsappNumber = "",
                    address = "",
                    businessType = "",
                    isOnboardingCompleted = false
                )
                dao.saveSettings(updated)
                updated
            } else {
                s
            }
        }
    }

    suspend fun saveSettings(settings: StoreSettings) {
        withContext(Dispatchers.IO) {
            dao.saveSettings(settings)
        }
    }

    suspend fun insertOrUpdateProduct(product: Product): Long {
        return withContext(Dispatchers.IO) {
            if (product.id == 0L) {
                dao.insertProduct(product)
            } else {
                dao.updateProduct(product)
                product.id
            }
        }
    }

    suspend fun deleteProduct(product: Product) {
        withContext(Dispatchers.IO) {
            dao.deleteProduct(product)
        }
    }

    suspend fun getProductByBarcode(barcode: String): Product? {
        return withContext(Dispatchers.IO) {
            dao.getProductByBarcode(barcode)
        }
    }

    suspend fun checkImportItemsStatus(items: List<BatchProductImportItem>): List<BatchProductImportItem> {
        return withContext(Dispatchers.IO) {
            items.map { item ->
                var existing: Product? = null
                if (item.barcode.isNotBlank()) {
                    existing = dao.getProductByBarcode(item.barcode)
                }
                if (existing == null) {
                    existing = dao.getProductByNameSync(item.name.trim())
                }

                if (existing != null) {
                    item.copy(
                        isExisting = true,
                        existingProductId = existing.id,
                        currentStock = existing.stockQuantity,
                        category = if (item.category.isBlank() || item.category == "General") existing.category else item.category
                    )
                } else {
                    item.copy(isExisting = false)
                }
            }
        }
    }

    suspend fun batchImportProducts(
        items: List<BatchProductImportItem>,
        addToExistingStock: Boolean,
        updatePrices: Boolean
    ): BatchImportResult {
        return withContext(Dispatchers.IO) {
            var added = 0
            var updated = 0
            val errors = mutableListOf<String>()

            for (item in items) {
                try {
                    if (item.name.isBlank()) continue

                    var existing: Product? = null
                    if (item.barcode.isNotBlank()) {
                        existing = dao.getProductByBarcode(item.barcode)
                    }
                    if (existing == null) {
                        existing = dao.getProductByNameSync(item.name.trim())
                    }

                    if (existing != null) {
                        val newStock = if (addToExistingStock) existing.stockQuantity + item.stockQuantity else item.stockQuantity
                        val newPurchase = if (updatePrices && item.purchasePrice > 0) item.purchasePrice else existing.purchasePrice
                        val newSale = if (updatePrices && item.salePrice > 0) item.salePrice else existing.salePrice

                        val updatedProduct = existing.copy(
                            stockQuantity = newStock,
                            purchasePrice = newPurchase,
                            salePrice = newSale,
                            category = if (existing.category.isBlank() || existing.category == "General") item.category else existing.category,
                            unit = if (item.unit.isNotBlank()) item.unit else existing.unit
                        )
                        dao.updateProduct(updatedProduct)
                        updated++
                    } else {
                        val newProduct = Product(
                            name = item.name.trim(),
                            category = if (item.category.isBlank()) "General" else item.category.trim(),
                            purchasePrice = item.purchasePrice,
                            salePrice = item.salePrice,
                            stockQuantity = item.stockQuantity,
                            unit = if (item.unit.isBlank()) "Pcs" else item.unit.trim(),
                            barcode = item.barcode.trim(),
                            minStockLevel = if (item.minStockLevel > 0) item.minStockLevel else 5.0
                        )
                        dao.insertProduct(newProduct)
                        added++

                        if (newProduct.category.isNotBlank()) {
                            dao.insertCategory(CategoryEntity(name = newProduct.category))
                        }
                    }
                } catch (e: Exception) {
                    errors.add("Error importing ${item.name}: ${e.message}")
                }
            }

            BatchImportResult(addedCount = added, updatedCount = updated, errors = errors)
        }
    }

    // --- CUSTOMER OPERATIONS ---
    suspend fun saveCustomer(customer: Customer): Long {
        return withContext(Dispatchers.IO) {
            if (customer.id == 0L) {
                dao.insertCustomer(customer)
            } else {
                dao.updateCustomer(customer)
                customer.id
            }
        }
    }

    suspend fun deleteCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            dao.deleteCustomer(customer)
        }
    }

    fun getCustomerLedgers(customerId: Long): Flow<List<CustomerLedger>> =
        dao.getCustomerLedgers(customerId)

    suspend fun recordCustomerPayment(customerId: Long, amount: Double, notes: String, paymentMethod: String = "Cash") {
        withContext(Dispatchers.IO) {
            val customer = dao.getCustomerById(customerId) ?: return@withContext
            val newBalance = customer.balance - amount
            dao.updateCustomerBalance(customerId, -amount)
            dao.insertCustomerLedger(
                CustomerLedger(
                    customerId = customerId,
                    description = if (notes.isBlank()) "Payment Received ($paymentMethod)" else "$notes ($paymentMethod)",
                    type = "PAYMENT_RECEIVED",
                    amount = amount,
                    balanceAfter = newBalance,
                    paymentMethod = paymentMethod
                )
            )
        }
    }

    suspend fun addCustomerDebitAdjustment(customerId: Long, amount: Double, notes: String) {
        withContext(Dispatchers.IO) {
            val customer = dao.getCustomerById(customerId) ?: return@withContext
            val newBalance = customer.balance + amount
            dao.updateCustomerBalance(customerId, amount)
            dao.insertCustomerLedger(
                CustomerLedger(
                    customerId = customerId,
                    description = if (notes.isBlank()) "Manual Udhaar Charge" else notes,
                    type = "DEBIT_ADJUSTMENT",
                    amount = amount,
                    balanceAfter = newBalance
                )
            )
        }
    }

    // --- SUPPLIER OPERATIONS ---
    suspend fun saveSupplier(supplier: Supplier): Long {
        return withContext(Dispatchers.IO) {
            if (supplier.id == 0L) {
                dao.insertSupplier(supplier)
            } else {
                dao.updateSupplier(supplier)
                supplier.id
            }
        }
    }

    suspend fun deleteSupplier(supplier: Supplier) {
        withContext(Dispatchers.IO) {
            dao.deleteSupplier(supplier)
        }
    }

    fun getSupplierLedgers(supplierId: Long): Flow<List<SupplierLedger>> =
        dao.getSupplierLedgers(supplierId)

    suspend fun recordSupplierPayment(supplierId: Long, amount: Double, notes: String, paymentMethod: String = "Cash") {
        withContext(Dispatchers.IO) {
            val supplier = dao.getSupplierById(supplierId) ?: return@withContext
            val newBalance = supplier.payableBalance - amount
            dao.updateSupplierBalance(supplierId, -amount)
            dao.insertSupplierLedger(
                SupplierLedger(
                    supplierId = supplierId,
                    description = if (notes.isBlank()) "Payment Made ($paymentMethod)" else "$notes ($paymentMethod)",
                    type = "PAYMENT_MADE",
                    amount = amount,
                    balanceAfter = newBalance,
                    paymentMethod = paymentMethod
                )
            )
        }
    }

    suspend fun addSupplierCreditAdjustment(supplierId: Long, amount: Double, notes: String) {
        withContext(Dispatchers.IO) {
            val supplier = dao.getSupplierById(supplierId) ?: return@withContext
            val newBalance = supplier.payableBalance + amount
            dao.updateSupplierBalance(supplierId, amount)
            dao.insertSupplierLedger(
                SupplierLedger(
                    supplierId = supplierId,
                    description = if (notes.isBlank()) "Manual Payable Adjustment" else notes,
                    type = "CREDIT_ADJUSTMENT",
                    amount = amount,
                    balanceAfter = newBalance
                )
            )
        }
    }

    // --- POS SALE COMPLETION ---
    suspend fun completeSale(
        sale: Sale,
        items: List<SaleItem>
    ): Long {
        return withContext(Dispatchers.IO) {
            val saleId = dao.insertSale(sale)
            val preparedItems = items.map { it.copy(saleId = saleId) }
            dao.insertSaleItems(preparedItems)

            // Reduce stock for sold products
            for (item in items) {
                dao.updateStock(item.productId, -item.quantity)
            }

            // Record customer credit balance & ledger if unpaid portion exists
            if (sale.customerId != null && sale.customerId > 0 && sale.dueAmount > 0) {
                val customer = dao.getCustomerById(sale.customerId)
                if (customer != null) {
                    val newBalance = customer.balance + sale.dueAmount
                    dao.updateCustomerBalance(sale.customerId, sale.dueAmount)
                    dao.insertCustomerLedger(
                        CustomerLedger(
                            customerId = sale.customerId,
                            description = "Credit Sale Inv #${sale.invoiceNumber}",
                            type = "CREDIT_SALE",
                            amount = sale.dueAmount,
                            balanceAfter = newBalance
                        )
                    )
                }
            }

            saleId
        }
    }

    fun getSaleItems(saleId: Long): Flow<List<SaleItem>> = dao.getSaleItems(saleId)

    suspend fun getSaleItemsSync(saleId: Long): List<SaleItem> = dao.getSaleItemsSync(saleId)
    suspend fun getSaleById(saleId: Long): Sale? = dao.getSaleById(saleId)

    suspend fun deleteSale(sale: Sale): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val items = dao.getSaleItemsSync(sale.id)
                for (item in items) {
                    dao.updateStock(item.productId, item.quantity)
                }

                if (sale.customerId != null && sale.customerId > 0 && sale.dueAmount > 0) {
                    val customer = dao.getCustomerById(sale.customerId)
                    if (customer != null) {
                        val newBalance = (customer.balance - sale.dueAmount).coerceAtLeast(0.0)
                        dao.updateCustomerBalance(sale.customerId, -sale.dueAmount)
                        dao.insertCustomerLedger(
                            CustomerLedger(
                                customerId = sale.customerId,
                                description = "Reversal (Deleted Invoice #${sale.invoiceNumber})",
                                type = "DELETED_SALE",
                                amount = -sale.dueAmount,
                                balanceAfter = newBalance
                            )
                        )
                    }
                }

                dao.deleteSaleItemsBySaleId(sale.id)
                dao.deleteSale(sale.id)

                dao.insertRecycleBinItem(
                    RecycleBinItem(
                        itemType = "INVOICE",
                        originalId = sale.id,
                        title = "Invoice #${sale.invoiceNumber}",
                        subtitle = "Customer: ${sale.customerName} • Total: ${sale.netAmount}",
                        deletedAt = System.currentTimeMillis()
                    )
                )

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // --- PURCHASE ENTRY ---
    suspend fun completePurchase(
        purchase: Purchase,
        items: List<PurchaseItem>
    ): Long {
        return withContext(Dispatchers.IO) {
            val purchaseId = dao.insertPurchase(purchase)
            val preparedItems = items.map { it.copy(purchaseId = purchaseId) }
            dao.insertPurchaseItems(preparedItems)

            // Increase stock for purchased products
            for (item in items) {
                dao.updateStock(item.productId, item.quantity)
            }

            // Record supplier payable balance & ledger if unpaid balance exists
            if (purchase.supplierId != null && purchase.supplierId > 0 && purchase.dueAmount > 0) {
                val supplier = dao.getSupplierById(purchase.supplierId)
                if (supplier != null) {
                    val newBalance = supplier.payableBalance + purchase.dueAmount
                    dao.updateSupplierBalance(purchase.supplierId, purchase.dueAmount)
                    dao.insertSupplierLedger(
                        SupplierLedger(
                            supplierId = purchase.supplierId,
                            description = "Purchase #${purchase.purchaseNumber}",
                            type = "PURCHASE",
                            amount = purchase.dueAmount,
                            balanceAfter = newBalance
                        )
                    )
                }
            }

            purchaseId
        }
    }

    // --- RECYCLE BIN ---
    val allRecycleBinItems: Flow<List<RecycleBinItem>> = dao.getAllRecycleBinItems()

    suspend fun moveToRecycleBin(item: RecycleBinItem) {
        withContext(Dispatchers.IO) {
            dao.insertRecycleBinItem(item)
        }
    }

    suspend fun deleteRecycleBinItem(id: Long) {
        withContext(Dispatchers.IO) {
            dao.deleteRecycleBinItemById(id)
        }
    }

    suspend fun clearRecycleBin() {
        withContext(Dispatchers.IO) {
            dao.clearRecycleBin()
        }
    }

    suspend fun getUserCountSync(): Int = dao.getUserCountSync()
    suspend fun getStoreCountSync(): Int = dao.getStoreCountSync()

    suspend fun clearAllDataForReset() {
        withContext(Dispatchers.IO) {
            dao.clearProducts()
            dao.clearCustomers()
            dao.clearSuppliers()
            dao.clearSales()
            dao.clearSaleItems()
            dao.clearPurchases()
            dao.clearPurchaseItems()
            dao.clearExpenses()
            dao.clearUsers()
            dao.clearDailyClosings()
            dao.clearActivityLogs()
            dao.clearRecycleBin()
            dao.clearAllUserStorePermissions()
            dao.clearAllStores()
            dao.saveSettings(com.example.data.entity.StoreSettings(isOnboardingCompleted = false))
        }
    }

    // --- SEED SAMPLE DATA ---
    suspend fun seedSampleDataIfEmpty() {
        withContext(Dispatchers.IO) {
            // Seed Stores if empty
            val storeCount = dao.getStoreCount().firstOrNull() ?: 0
            if (storeCount == 0) {
                val sec1 = "SEC-" + (100000..999999).random()
                val qr1 = SecurityUtils.generateQrPayload(1L, "MAIN", sec1)

                dao.insertStore(
                    com.example.data.entity.StoreProfile(
                        id = 1L,
                        storeName = "Main Branch Store",
                        ownerName = "",
                        phone = "",
                        address = "",
                        code = "MAIN",
                        isPrimary = true,
                        secretCode = sec1,
                        qrCode = qr1,
                        secretCodeHash = SecurityUtils.hashSha256(sec1),
                        qrCodeHash = SecurityUtils.hashSha256(qr1),
                        isAccessCodeEnabled = true,
                        isLocked = false
                    )
                )
            }

            // Seed Categories
            val catCount = dao.getCategoryCount().firstOrNull() ?: 0
            // Seed User Accounts if empty
            val userCount = dao.getUserCount().firstOrNull() ?: 0
            if (userCount == 0) {
                dao.insertUser(
                    UserAccount(
                        username = "superadmin",
                        name = "Super Admin Owner",
                        pinCode = "1234",
                        role = "SUPER_ADMIN",
                        phone = ""
                    )
                )
                dao.insertUser(
                    UserAccount(
                        username = "admin",
                        name = "Store Manager",
                        pinCode = "5678",
                        role = "ADMIN",
                        phone = ""
                    )
                )
                dao.insertUser(
                    UserAccount(
                        username = "staff",
                        name = "Sales Staff",
                        pinCode = "0000",
                        role = "EMPLOYEE",
                        phone = ""
                    )
                )
            }

            if (catCount == 0) {
                val standardCategories = listOf(
                    CategoryEntity(name = "Plumbing", description = "PVC, PPRC, CPVC Pipes & Fitting Accessories"),
                    CategoryEntity(name = "Paint & Finishes", description = "Wall Paints, Enamels, Primers & Spray Paints"),
                    CategoryEntity(name = "Tools", description = "Hand Tools, Power Tools, Drill Bits & Measurement"),
                    CategoryEntity(name = "Building Materials", description = "Cement, Tiles, Bricks, Steel & Chemicals"),
                    CategoryEntity(name = "Sanitary", description = "Commode Seats, Wash Basins, Faucets & Sinks"),
                    CategoryEntity(name = "Roofing & Insulation", description = "Waterproofing Sheets, Heat Insulation & Roofing Material"),
                    CategoryEntity(name = "Hardware & Fittings", description = "Door Locks, Hinges, Screws, Nails & Brackets"),
                    CategoryEntity(name = "Adhesives & Chemicals", description = "Silicon, Epoxy, Water Proofing & Solvents")
                )
                for (cat in standardCategories) {
                    dao.insertCategory(cat)
                }
            }

            val count = dao.getProductCount().firstOrNull() ?: 0
            if (count > 0) return@withContext

            // Save default settings
            dao.saveSettings(StoreSettings())

            // Seed Customers
            val c1 = dao.insertCustomer(Customer(name = "Contractor Ali Raza", phone = "0301-8889900", address = "Site 14, Commercial Zone", balance = 18500.0))
            val c2 = dao.insertCustomer(Customer(name = "Builder Usman Ghani", phone = "0322-5554433", address = "Sector H-12 Plaza", balance = 0.0))
            val c3 = dao.insertCustomer(Customer(name = "Architect Tariq Mahmood", phone = "0333-1112233", address = "Main Boulevard", balance = 4200.0))

            // Seed Suppliers
            val s1 = dao.insertSupplier(Supplier(name = "Master Sanitary Fittings", company = "Master Sanitary Pak", phone = "042-3551122", address = "Industrial Area, Gujranwala", payableBalance = 45000.0))
            val s2 = dao.insertSupplier(Supplier(name = "Popular Pipe Industries", company = "Popular Group", phone = "0300-4445566", address = "Faisalabad Road", payableBalance = 0.0))
            val s3 = dao.insertSupplier(Supplier(name = "Berger Paints Pakistan", company = "Berger Paints Ltd", phone = "042-3654123", address = "Multan Road, Lahore", payableBalance = 28500.0))
            val s4 = dao.insertSupplier(Supplier(name = "Maple Leaf Cement Mills", company = "Kohinoor Maple Leaf", phone = "042-11133344", address = "Daudkhel", payableBalance = 62000.0))

            // Seed Hardware, Sanitary, Plumbing & Building Material Products
            val sampleProducts = listOf(
                Product(barcode = "89650001", name = "PVC Pipe 3\" Heavy Duty (10ft)", category = "Plumbing & Pipes", purchasePrice = 620.0, salePrice = 750.0, stockQuantity = 50.0, minStockLevel = 10.0, unit = "Ft"),
                Product(barcode = "89650002", name = "PPRC Pipe 25mm PN20 Green", category = "Plumbing & Pipes", purchasePrice = 350.0, salePrice = 430.0, stockQuantity = 80.0, minStockLevel = 15.0, unit = "Meter"),
                Product(barcode = "89650003", name = "Luxury Bath Set 7-Piece Chrome", category = "Sanitary", purchasePrice = 8500.0, salePrice = 10500.0, stockQuantity = 10.0, minStockLevel = 3.0, unit = "Set"),
                Product(barcode = "89650004", name = "Ceramic Wash Basin Wall Mount", category = "Sanitary", purchasePrice = 3200.0, salePrice = 3900.0, stockQuantity = 15.0, minStockLevel = 4.0, unit = "Pcs"),
                Product(barcode = "89650005", name = "Master One-Piece Commode Seat", category = "Sanitary", purchasePrice = 14500.0, salePrice = 17500.0, stockQuantity = 6.0, minStockLevel = 2.0, unit = "Pcs"),
                Product(barcode = "89650006", name = "Stainless Steel Kitchen Sink Double Bowl", category = "Sanitary", purchasePrice = 6800.0, salePrice = 8200.0, stockQuantity = 8.0, minStockLevel = 2.0, unit = "Pcs"),
                Product(barcode = "89650007", name = "Brass Water Tap 1/2\" Heavy Mix", category = "Sanitary", purchasePrice = 750.0, salePrice = 950.0, stockQuantity = 35.0, minStockLevel = 8.0, unit = "Pcs"),
                Product(barcode = "89650008", name = "High Pressure Rain Shower Set", category = "Sanitary", purchasePrice = 2800.0, salePrice = 3500.0, stockQuantity = 12.0, minStockLevel = 3.0, unit = "Set"),
                Product(barcode = "89650009", name = "Plastic Water Tank 1000 Liters", category = "Plumbing & Pipes", purchasePrice = 12500.0, salePrice = 14800.0, stockQuantity = 2.0, minStockLevel = 4.0, unit = "Pcs"), // Low stock alert!
                Product(barcode = "89650010", name = "Berger Weathercoat Smooth Matt Paint 4L", category = "Paint & Finishes", purchasePrice = 3800.0, salePrice = 4500.0, stockQuantity = 35.0, minStockLevel = 8.0, unit = "Pcs"),
                Product(barcode = "89650011", name = "Dulux Gloss Enamel Super White 3.64L", category = "Paint & Finishes", purchasePrice = 3200.0, salePrice = 3850.0, stockQuantity = 25.0, minStockLevel = 5.0, unit = "Pcs"),
                Product(barcode = "89650012", name = "Deformed Steel Rebar 12mm 60-Grade (40ft)", category = "Building Materials", purchasePrice = 4800.0, salePrice = 5400.0, stockQuantity = 20.0, minStockLevel = 5.0, unit = "Length"),
                Product(barcode = "89650013", name = "Sika Waterproofing Chemical Compound 1L", category = "Adhesives & Chemicals", purchasePrice = 850.0, salePrice = 1100.0, stockQuantity = 4.0, minStockLevel = 10.0, unit = "Pcs"), // Low stock alert!
                Product(barcode = "89650014", name = "Master Weather Wall Paint White 4L", category = "Paints & Adhesives", purchasePrice = 2400.0, salePrice = 2900.0, stockQuantity = 20.0, minStockLevel = 5.0, unit = "Pcs"),
                Product(barcode = "89650015", name = "Maple Leaf OPC Cement 50kg", category = "Building Material", purchasePrice = 1220.0, salePrice = 1300.0, stockQuantity = 150.0, minStockLevel = 30.0, unit = "Bag"),
                Product(barcode = "89650016", name = "Porcelain Floor Tiles 60x60cm", category = "Building Material", purchasePrice = 2100.0, salePrice = 2500.0, stockQuantity = 60.0, minStockLevel = 15.0, unit = "Box"),
                Product(barcode = "89650017", name = "Heavy Duty Mortise Door Lock Handle", category = "Hardware & Tools", purchasePrice = 1850.0, salePrice = 2300.0, stockQuantity = 25.0, minStockLevel = 5.0, unit = "Pcs"),
                Product(barcode = "89650018", name = "Professional Combination Pliers 8\"", category = "Hardware & Tools", purchasePrice = 650.0, salePrice = 850.0, stockQuantity = 18.0, minStockLevel = 5.0, unit = "Pcs"),
                Product(barcode = "89650019", name = "Magic Bond Epoxy Adhesive 100g", category = "Paints & Adhesives", purchasePrice = 180.0, salePrice = 240.0, stockQuantity = 45.0, minStockLevel = 10.0, unit = "Pcs"),
                Product(barcode = "89650020", name = "Brass Ball Valve 1\" Heavy Duty", category = "Plumbing & Pipes", purchasePrice = 880.0, salePrice = 1100.0, stockQuantity = 30.0, minStockLevel = 8.0, unit = "Pcs")
            )

            for (p in sampleProducts) {
                dao.insertProduct(p)
            }

            // Seed an initial sample sale
            val saleId = dao.insertSale(
                Sale(
                    invoiceNumber = "INV-1001",
                    customerId = c1,
                    customerName = "Contractor Ali Raza",
                    totalAmount = 21000.0,
                    discount = 2500.0,
                    netAmount = 18500.0,
                    paidAmount = 0.0,
                    dueAmount = 18500.0,
                    paymentType = "Udhaar / Credit",
                    timestamp = System.currentTimeMillis() - 3600000
                )
            )

            dao.insertSaleItems(
                listOf(
                    SaleItem(saleId = saleId, productId = 3, productName = "Luxury Bath Set 7-Piece Chrome", quantity = 1.0, unit = "Set", purchasePrice = 8500.0, salePrice = 10500.0, totalPrice = 10500.0),
                    SaleItem(saleId = saleId, productId = 5, productName = "Master One-Piece Commode Seat", quantity = 1.0, unit = "Pcs", purchasePrice = 14500.0, salePrice = 17500.0, totalPrice = 17500.0)
                )
            )

            dao.insertCustomerLedger(
                CustomerLedger(
                    customerId = c1,
                    description = "Credit Sale Inv #INV-1001",
                    type = "CREDIT_SALE",
                    amount = 18500.0,
                    balanceAfter = 18500.0,
                    timestamp = System.currentTimeMillis() - 3600000
                )
            )
        }
    }

    // --- ATTENDANCE MANAGEMENT ---
    val allAttendanceRecords: Flow<List<AttendanceRecord>> = dao.getAllAttendanceRecords()

    fun getAttendanceForStore(storeId: Long): Flow<List<AttendanceRecord>> = dao.getAttendanceRecordsForStore(storeId)

    fun getAttendanceForUser(userId: Long): Flow<List<AttendanceRecord>> = dao.getAttendanceRecordsForUser(userId)

    fun getAttendanceForDate(dateStr: String): Flow<List<AttendanceRecord>> = dao.getAttendanceRecordsForDate(dateStr)

    suspend fun getAttendanceForUserAndDate(userId: Long, dateStr: String): AttendanceRecord? {
        return withContext(Dispatchers.IO) {
            dao.getAttendanceForUserAndDate(userId, dateStr)
        }
    }

    suspend fun saveAttendanceRecord(record: AttendanceRecord): Long {
        return withContext(Dispatchers.IO) {
            dao.insertAttendanceRecord(record)
        }
    }

    suspend fun updateAttendanceRecord(record: AttendanceRecord) {
        withContext(Dispatchers.IO) {
            dao.updateAttendanceRecord(record)
        }
    }

    suspend fun deleteAttendanceRecord(record: AttendanceRecord) {
        withContext(Dispatchers.IO) {
            dao.deleteAttendanceRecord(record)
        }
    }
}
