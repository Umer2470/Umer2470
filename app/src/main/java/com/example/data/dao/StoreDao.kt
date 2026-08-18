package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.Customer
import com.example.data.entity.CustomerLedger
import com.example.data.entity.CategoryEntity
import com.example.data.entity.Expense
import com.example.data.entity.Product
import com.example.data.entity.Purchase
import com.example.data.entity.PurchaseItem
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import com.example.data.entity.Supplier
import com.example.data.entity.SupplierLedger
import kotlinx.coroutines.flow.Flow

import com.example.data.entity.TenantAccount
import com.example.data.entity.SubscriptionPlan
import com.example.data.entity.UserAccount
import com.example.data.entity.DailyClosing
import com.example.data.entity.ActivityLog
import com.example.data.entity.StoreProfile
import com.example.data.entity.UserStorePermission
import com.example.data.entity.RecycleBinItem
import com.example.data.entity.SuperAdminRecovery
import com.example.data.entity.AttendanceRecord

@Dao
interface StoreDao {

    // --- MULTI-TENANT SAAS ACCOUNTS & LICENSING ---
    @Query("SELECT * FROM tenant_accounts ORDER BY createdAt DESC")
    fun getAllTenants(): Flow<List<TenantAccount>>

    @Query("SELECT * FROM tenant_accounts WHERE id = :id LIMIT 1")
    suspend fun getTenantById(id: String): TenantAccount?

    @Query("SELECT * FROM tenant_accounts WHERE tenantCode = :code LIMIT 1")
    suspend fun getTenantByCode(code: String): TenantAccount?

    @Query("SELECT * FROM tenant_accounts WHERE licenseKey = :key LIMIT 1")
    suspend fun getTenantByLicenseKey(key: String): TenantAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenant(tenant: TenantAccount)

    @Update
    suspend fun updateTenant(tenant: TenantAccount)

    @Delete
    suspend fun deleteTenant(tenant: TenantAccount)

    @Query("DELETE FROM tenant_accounts WHERE id = :id")
    suspend fun deleteTenantById(id: String)

    @Query("SELECT COUNT(*) FROM tenant_accounts WHERE isMasterOwnerAccount = 0")
    fun getTenantCount(): Flow<Int>

    // --- SUBSCRIPTION PLANS ---
    @Query("SELECT * FROM saas_subscription_plans")
    fun getAllSubscriptionPlans(): Flow<List<SubscriptionPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptionPlan(plan: SubscriptionPlan)

    @Query("DELETE FROM stores WHERE tenantId = :tenantId")
    suspend fun clearStoresForTenant(tenantId: String)

    @Query("DELETE FROM products WHERE tenantId = :tenantId")
    suspend fun clearProductsForTenant(tenantId: String)

    @Query("DELETE FROM app_users WHERE tenantId = :tenantId")
    suspend fun clearUsersForTenant(tenantId: String)

    // --- STORES ---
    @Query("SELECT * FROM stores ORDER BY id ASC")
    fun getAllStores(): Flow<List<StoreProfile>>

    @Query("SELECT * FROM stores WHERE id = :id LIMIT 1")
    suspend fun getStoreById(id: Long): StoreProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: StoreProfile): Long

    @Update
    suspend fun updateStore(store: StoreProfile)

    @Delete
    suspend fun deleteStore(store: StoreProfile)

    @Query("SELECT COUNT(*) FROM stores")
    fun getStoreCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM stores")
    suspend fun getStoreCountSync(): Int

    // --- STORE SETTINGS ---
    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<StoreSettings?>

    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): StoreSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: StoreSettings)

    // --- CATEGORIES ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("SELECT COUNT(*) FROM categories")
    fun getCategoryCount(): Flow<Int>

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    // --- PRODUCTS / INVENTORY ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE stockQuantity <= minStockLevel ORDER BY stockQuantity ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE stockQuantity <= minStockLevel OR stockQuantity <= :defaultThreshold ORDER BY stockQuantity ASC")
    fun getLowStockProductsWithThreshold(defaultThreshold: Double): Flow<List<Product>>

    @Query("UPDATE products SET minStockLevel = :threshold WHERE minStockLevel < :threshold")
    suspend fun updateMinStockThresholds(threshold: Double)

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getProductByNameSync(name: String): Product?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET stockQuantity = stockQuantity + :qtyChange WHERE id = :productId")
    suspend fun updateStock(productId: Long, qtyChange: Double)

    @Query("SELECT COUNT(*) FROM products")
    fun getProductCount(): Flow<Int>

    // --- CUSTOMERS ---
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("UPDATE customers SET balance = balance + :amountChange WHERE id = :customerId")
    suspend fun updateCustomerBalance(customerId: Long, amountChange: Double)

    @Query("SELECT * FROM customer_ledgers WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getCustomerLedgers(customerId: Long): Flow<List<CustomerLedger>>

    @Insert
    suspend fun insertCustomerLedger(ledger: CustomerLedger)

    // --- SUPPLIERS ---
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: Long): Supplier?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Delete
    suspend fun deleteSupplier(supplier: Supplier)

    @Query("UPDATE suppliers SET payableBalance = payableBalance + :amountChange WHERE id = :supplierId")
    suspend fun updateSupplierBalance(supplierId: Long, amountChange: Double)

    @Query("SELECT * FROM supplier_ledgers WHERE supplierId = :supplierId ORDER BY timestamp DESC")
    fun getSupplierLedgers(supplierId: Long): Flow<List<SupplierLedger>>

    @Insert
    suspend fun insertSupplierLedger(ledger: SupplierLedger)

    // --- SALES ---
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getSalesByDateRange(startTime: Long, endTime: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :saleId LIMIT 1")
    suspend fun getSaleById(saleId: Long): Sale?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getSaleItems(saleId: Long): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItemsSync(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sale_items")
    fun getAllSaleItems(): Flow<List<SaleItem>>

    @Insert
    suspend fun insertSale(sale: Sale): Long

    @Insert
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Query("DELETE FROM sales WHERE id = :saleId")
    suspend fun deleteSale(saleId: Long)

    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    suspend fun deleteSaleItemsBySaleId(saleId: Long)

    @Query("SELECT COUNT(*) FROM sales")
    fun getSaleCount(): Flow<Int>

    // --- PURCHASES ---
    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    fun getAllPurchases(): Flow<List<Purchase>>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    fun getPurchaseItems(purchaseId: Long): Flow<List<PurchaseItem>>

    @Insert
    suspend fun insertPurchase(purchase: Purchase): Long

    @Insert
    suspend fun insertPurchaseItems(items: List<PurchaseItem>)

    // --- EXPENSES ---
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getExpensesByDateRange(startTime: Long, endTime: Long): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // --- APP USER ACCOUNTS ---
    @Query("SELECT * FROM app_users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserAccount>>

    @Query("SELECT * FROM app_users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserAccount?

    @Query("SELECT * FROM app_users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserAccount?

    @Query("SELECT * FROM app_users WHERE pinCode = :pin AND isActive = 1 LIMIT 1")
    suspend fun getUserByPin(pin: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount): Long

    @Update
    suspend fun updateUser(user: UserAccount)

    @Delete
    suspend fun deleteUser(user: UserAccount)

    @Query("SELECT COUNT(*) FROM app_users")
    fun getUserCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_users")
    suspend fun getUserCountSync(): Int

    // --- DAILY CLOSINGS ---
    @Query("SELECT * FROM daily_closings ORDER BY closeTimestamp DESC")
    fun getAllDailyClosings(): Flow<List<DailyClosing>>

    @Query("SELECT * FROM daily_closings WHERE userId = :userId ORDER BY closeTimestamp DESC")
    fun getDailyClosingsByUserId(userId: Long): Flow<List<DailyClosing>>

    @Query("SELECT * FROM daily_closings WHERE userId = :userId AND status = 'OPEN' ORDER BY openTimestamp DESC LIMIT 1")
    suspend fun getOpenDailyClosingForUser(userId: Long): DailyClosing?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyClosing(closing: DailyClosing): Long

    @Update
    suspend fun updateDailyClosing(closing: DailyClosing)

    // --- ACTIVITY LOGS ---
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 300")
    fun getAllActivityLogs(): Flow<List<ActivityLog>>

    @Insert
    suspend fun insertActivityLog(log: ActivityLog): Long

    // Clear all for restore/reset if needed
    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM customers")
    suspend fun clearCustomers()

    @Query("DELETE FROM suppliers")
    suspend fun clearSuppliers()

    @Query("DELETE FROM sales")
    suspend fun clearSales()

    @Query("DELETE FROM sale_items")
    suspend fun clearSaleItems()

    @Query("DELETE FROM purchases")
    suspend fun clearPurchases()

    @Query("DELETE FROM purchase_items")
    suspend fun clearPurchaseItems()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM app_users")
    suspend fun clearUsers()

    @Query("DELETE FROM daily_closings")
    suspend fun clearDailyClosings()

    @Query("DELETE FROM activity_logs")
    suspend fun clearActivityLogs()

    // --- USER STORE PERMISSIONS ---
    @Query("SELECT * FROM user_store_permissions WHERE userId = :userId AND isGranted = 1")
    fun getUserPermissions(userId: Long): Flow<List<UserStorePermission>>

    @Query("SELECT * FROM user_store_permissions WHERE userId = :userId AND storeId = :storeId LIMIT 1")
    suspend fun getUserPermissionForStore(userId: Long, storeId: Long): UserStorePermission?

    @Query("SELECT * FROM user_store_permissions WHERE storeId = :storeId AND isGranted = 1")
    fun getPermissionsForStore(storeId: Long): Flow<List<UserStorePermission>>

    @Query("SELECT * FROM user_store_permissions WHERE isGranted = 1")
    fun getAllUserStorePermissions(): Flow<List<UserStorePermission>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserPermission(permission: UserStorePermission): Long

    @Query("DELETE FROM user_store_permissions WHERE userId = :userId AND storeId = :storeId")
    suspend fun revokeUserStorePermission(userId: Long, storeId: Long)

    @Query("DELETE FROM user_store_permissions WHERE storeId = :storeId")
    suspend fun clearPermissionsForStore(storeId: Long)

    @Query("DELETE FROM user_store_permissions")
    suspend fun clearAllUserStorePermissions()

    @Query("DELETE FROM stores")
    suspend fun clearAllStores()

    // --- RECYCLE BIN ---
    @Query("SELECT * FROM recycle_bin ORDER BY deletedAt DESC")
    fun getAllRecycleBinItems(): Flow<List<RecycleBinItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecycleBinItem(item: RecycleBinItem): Long

    @Query("DELETE FROM recycle_bin WHERE id = :id")
    suspend fun deleteRecycleBinItemById(id: Long)

    @Query("DELETE FROM recycle_bin")
    suspend fun clearRecycleBin()

    // --- SUPER ADMIN RECOVERY SYSTEM ---
    @Query("SELECT * FROM super_admin_recovery WHERE id = 1 LIMIT 1")
    fun getSuperAdminRecoveryFlow(): Flow<SuperAdminRecovery?>

    @Query("SELECT * FROM super_admin_recovery WHERE id = 1 LIMIT 1")
    suspend fun getSuperAdminRecoverySync(): SuperAdminRecovery?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuperAdminRecovery(recovery: SuperAdminRecovery)

    // --- ATTENDANCE MANAGEMENT ---
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE storeId = :storeId ORDER BY timestamp DESC")
    fun getAttendanceRecordsForStore(storeId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAttendanceRecordsForUser(userId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE dateStr = :dateStr")
    fun getAttendanceRecordsForDate(dateStr: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE userId = :userId AND dateStr = :dateStr LIMIT 1")
    suspend fun getAttendanceForUserAndDate(userId: Long, dateStr: String): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecord(record: AttendanceRecord): Long

    @Update
    suspend fun updateAttendanceRecord(record: AttendanceRecord)

    @Delete
    suspend fun deleteAttendanceRecord(record: AttendanceRecord)
}
