package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name ASC")
    suspend fun getAllProducts(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id AND isDeleted = 0")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND isDeleted = 0 LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteProduct(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM products WHERE isDeleted = 1")
    fun getRecycleBinProductsFlow(): Flow<List<Product>>

    @Query("UPDATE products SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreProduct(id: Long)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun hardDeleteProduct(id: Long)

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllSalesFlow(): Flow<List<Sale>>

    @Query("SELECT * FROM sales")
    suspend fun getAllSales(): List<Sale>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): Sale?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<Sale>)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sale_items")
    fun getAllSaleItemsFlow(): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllSaleItems(): List<SaleItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Update
    suspend fun updateSale(sale: Sale)

    @Query("UPDATE sales SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteSale(id: Long)

    @Query("SELECT * FROM sales WHERE isDeleted = 1")
    fun getRecycleBinSalesFlow(): Flow<List<Sale>>

    @Query("UPDATE sales SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreSale(id: Long)

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun hardDeleteSale(id: Long)

    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    suspend fun deleteItemsForSale(saleId: Long)

    @Query("DELETE FROM sales")
    suspend fun clearAllSales()

    @Query("DELETE FROM sale_items")
    suspend fun clearAllSaleItems()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllCustomersFlow(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY name ASC")
    suspend fun getAllCustomers(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE customers SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteCustomer(id: Long)

    @Query("SELECT * FROM customers WHERE isDeleted = 1")
    fun getRecycleBinCustomersFlow(): Flow<List<Customer>>

    @Query("UPDATE customers SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreCustomer(id: Long)

    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllSuppliersFlow(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers")
    suspend fun getAllSuppliers(): List<Supplier>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: Long): Supplier?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<Supplier>)

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Query("UPDATE suppliers SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteSupplier(id: Long)

    @Query("SELECT * FROM suppliers WHERE isDeleted = 1")
    fun getRecycleBinSuppliersFlow(): Flow<List<Supplier>>

    @Query("UPDATE suppliers SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreSupplier(id: Long)

    @Query("DELETE FROM suppliers")
    suspend fun clearAllSuppliers()
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllPurchasesFlow(): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases")
    suspend fun getAllPurchases(): List<Purchase>

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getPurchaseById(id: Long): Purchase?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchases(purchases: List<Purchase>)

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun getItemsForPurchase(purchaseId: Long): List<PurchaseItem>

    @Query("SELECT * FROM purchase_items")
    suspend fun getAllPurchaseItems(): List<PurchaseItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItem>)

    @Update
    suspend fun updatePurchase(purchase: Purchase)

    @Query("UPDATE purchases SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeletePurchase(id: Long)

    @Query("DELETE FROM purchases")
    suspend fun clearAllPurchases()

    @Query("DELETE FROM purchase_items")
    suspend fun clearAllPurchaseItems()
}

@Dao
interface StoreSettingsDao {
    @Query("SELECT * FROM store_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<StoreSettings?>

    @Query("SELECT * FROM store_settings WHERE id = 1")
    suspend fun getSettings(): StoreSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: StoreSettings)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY fullName ASC")
    fun getAllUsersFlow(): Flow<List<User>>

    @Query("SELECT * FROM users ORDER BY fullName ASC")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY fullName ASC")
    suspend fun getActiveUsers(): List<User>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("UPDATE users SET isActive = 0 WHERE id = :id")
    suspend fun deactivateUser(id: Long)

    @Query("DELETE FROM users")
    suspend fun clearAllUsers()
}

@Dao
interface StoreBranchDao {
    @Query("SELECT * FROM store_branches WHERE isActive = 1 ORDER BY id ASC")
    fun getAllBranchesFlow(): Flow<List<StoreBranch>>

    @Query("SELECT * FROM store_branches")
    suspend fun getAllBranches(): List<StoreBranch>

    @Query("SELECT * FROM store_branches WHERE id = :id LIMIT 1")
    suspend fun getBranchById(id: Long): StoreBranch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: StoreBranch): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranches(branches: List<StoreBranch>)

    @Update
    suspend fun updateBranch(branch: StoreBranch)

    @Delete
    suspend fun deleteBranch(branch: StoreBranch)

    @Query("UPDATE store_branches SET isActive = 0 WHERE id = :id")
    suspend fun deactivateBranch(id: Long)

    @Query("DELETE FROM store_branches")
    suspend fun clearAllBranches()
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY checkInTime DESC")
    fun getAllAttendanceFlow(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllAttendance(): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE dateString = :date ORDER BY checkInTime DESC")
    fun getAttendanceForDateFlow(date: String): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecord>)

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records")
    suspend fun clearAllAttendance()
}

@Dao
interface BusinessProfileDao {
    @Query("SELECT * FROM business_profiles WHERE id = 1")
    fun getProfileFlow(): Flow<BusinessProfile?>

    @Query("SELECT * FROM business_profiles WHERE id = 1")
    suspend fun getProfile(): BusinessProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: BusinessProfile)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 500")
    fun getRecentLogsFlow(): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs")
    suspend fun getAllLogs(): List<ActivityLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<ActivityLog>)

    @Query("DELETE FROM activity_logs")
    suspend fun clearAllLogs()
}

@Dao
interface PaymentQrConfigDao {
    @Query("SELECT * FROM payment_qr_configs ORDER BY displayOrder ASC, id ASC")
    fun getAllPaymentQrsFlow(): Flow<List<PaymentQrConfig>>

    @Query("SELECT * FROM payment_qr_configs ORDER BY displayOrder ASC, id ASC")
    suspend fun getAllPaymentQrs(): List<PaymentQrConfig>

    @Query("SELECT * FROM payment_qr_configs WHERE isEnabled = 1 ORDER BY isDefault DESC, displayOrder ASC, id ASC")
    fun getActivePaymentQrsFlow(): Flow<List<PaymentQrConfig>>

    @Query("SELECT * FROM payment_qr_configs WHERE id = :id LIMIT 1")
    suspend fun getPaymentQrById(id: Long): PaymentQrConfig?

    @Query("SELECT * FROM payment_qr_configs WHERE isDefault = 1 AND isEnabled = 1 LIMIT 1")
    suspend fun getDefaultPaymentQr(): PaymentQrConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentQr(config: PaymentQrConfig): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentQrs(configs: List<PaymentQrConfig>)

    @Update
    suspend fun updatePaymentQr(config: PaymentQrConfig)

    @Query("DELETE FROM payment_qr_configs WHERE id = :id")
    suspend fun deletePaymentQr(id: Long)

    @Query("UPDATE payment_qr_configs SET isDefault = CASE WHEN id = :defaultId THEN 1 ELSE 0 END")
    suspend fun setDefaultPaymentQr(defaultId: Long)

    @Query("DELETE FROM payment_qr_configs")
    suspend fun clearAllPaymentQrs()
}

@Dao
interface RegisterShiftDao {
    @Query("SELECT * FROM register_shifts WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    fun getActiveShiftFlow(): Flow<RegisterShift?>

    @Query("SELECT * FROM register_shifts WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    suspend fun getActiveShift(): RegisterShift?

    @Query("SELECT * FROM register_shifts ORDER BY openedAt DESC")
    fun getAllShiftsFlow(): Flow<List<RegisterShift>>

    @Query("SELECT * FROM register_shifts ORDER BY openedAt DESC")
    suspend fun getAllShifts(): List<RegisterShift>

    @Query("SELECT * FROM register_shifts WHERE id = :id LIMIT 1")
    suspend fun getShiftById(id: Long): RegisterShift?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: RegisterShift): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShifts(shifts: List<RegisterShift>)

    @Update
    suspend fun updateShift(shift: RegisterShift)

    @Query("DELETE FROM register_shifts WHERE id = :id")
    suspend fun deleteShift(id: Long)

    @Query("DELETE FROM register_shifts")
    suspend fun clearAllShifts()
}

@Dao
interface CashMovementDao {
    @Query("SELECT * FROM cash_movements WHERE shiftId = :shiftId ORDER BY timestamp ASC")
    fun getMovementsForShiftFlow(shiftId: Long): Flow<List<CashMovement>>

    @Query("SELECT * FROM cash_movements WHERE shiftId = :shiftId ORDER BY timestamp ASC")
    suspend fun getMovementsForShift(shiftId: Long): List<CashMovement>

    @Query("SELECT * FROM cash_movements ORDER BY timestamp DESC")
    fun getAllMovementsFlow(): Flow<List<CashMovement>>

    @Query("SELECT * FROM cash_movements ORDER BY timestamp DESC")
    suspend fun getAllMovements(): List<CashMovement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: CashMovement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<CashMovement>)

    @Query("DELETE FROM cash_movements WHERE id = :id")
    suspend fun deleteMovement(id: Long)

    @Query("DELETE FROM cash_movements WHERE shiftId = :shiftId")
    suspend fun deleteMovementsForShift(shiftId: Long)

    @Query("DELETE FROM cash_movements")
    suspend fun clearAllMovements()
}

@Dao
interface SaleReturnDao {
    @Query("SELECT * FROM sale_returns ORDER BY createdAt DESC")
    fun getAllReturnsFlow(): Flow<List<SaleReturn>>

    @Query("SELECT * FROM sale_returns ORDER BY createdAt DESC")
    suspend fun getAllReturns(): List<SaleReturn>

    @Query("SELECT * FROM sale_returns WHERE saleId = :saleId")
    suspend fun getReturnsForSale(saleId: Long): List<SaleReturn>

    @Query("SELECT * FROM sale_returns WHERE id = :id")
    suspend fun getReturnById(id: Long): SaleReturn?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(saleReturn: SaleReturn): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturns(returns: List<SaleReturn>)

    @Delete
    suspend fun deleteReturn(saleReturn: SaleReturn)

    @Query("DELETE FROM sale_returns")
    suspend fun clearAllReturns()
}

@Dao
interface SaleReturnItemDao {
    @Query("SELECT * FROM sale_return_items WHERE returnId = :returnId")
    suspend fun getItemsForReturn(returnId: Long): List<SaleReturnItem>

    @Query("SELECT * FROM sale_return_items WHERE returnId = :returnId")
    fun getItemsForReturnFlow(returnId: Long): Flow<List<SaleReturnItem>>

    @Query("SELECT * FROM sale_return_items")
    suspend fun getAllReturnItems(): List<SaleReturnItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnItems(items: List<SaleReturnItem>)

    @Query("DELETE FROM sale_return_items")
    suspend fun clearAllReturnItems()
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun getAllMovementsFlow(): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    suspend fun getAllMovements(): List<StockMovement>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC")
    fun getMovementsForProductFlow(productId: Long): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC")
    suspend fun getMovementsForProduct(productId: Long): List<StockMovement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<StockMovement>)

    @Query("DELETE FROM stock_movements")
    suspend fun clearAllMovements()
}

@Dao
interface FbrInvoiceRecordDao {
    @Query("SELECT * FROM fbr_invoice_records ORDER BY createdAt DESC")
    fun getAllRecordsFlow(): Flow<List<FbrInvoiceRecord>>

    @Query("SELECT * FROM fbr_invoice_records ORDER BY createdAt DESC")
    suspend fun getAllRecords(): List<FbrInvoiceRecord>

    @Query("SELECT * FROM fbr_invoice_records WHERE saleId = :saleId LIMIT 1")
    suspend fun getRecordForSale(saleId: Long): FbrInvoiceRecord?

    @Query("SELECT * FROM fbr_invoice_records WHERE saleId = :saleId LIMIT 1")
    fun getRecordForSaleFlow(saleId: Long): Flow<FbrInvoiceRecord?>

    @Query("SELECT * FROM fbr_invoice_records WHERE submissionStatus IN ('PENDING', 'RETRY_REQUIRED', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getPendingOrFailedRecords(): List<FbrInvoiceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: FbrInvoiceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<FbrInvoiceRecord>)

    @Update
    suspend fun updateRecord(record: FbrInvoiceRecord)

    @Query("DELETE FROM fbr_invoice_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    @Query("DELETE FROM fbr_invoice_records")
    suspend fun clearAllRecords()
}

