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
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllSalesFlow(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): Sale?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: Long): List<SaleItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Query("UPDATE sales SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteSale(id: Long)

    @Query("SELECT * FROM sales WHERE isDeleted = 1")
    fun getRecycleBinSalesFlow(): Flow<List<Sale>>

    @Query("UPDATE sales SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreSale(id: Long)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllCustomersFlow(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE customers SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteCustomer(id: Long)

    @Query("SELECT * FROM customers WHERE isDeleted = 1")
    fun getRecycleBinCustomersFlow(): Flow<List<Customer>>

    @Query("UPDATE customers SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreCustomer(id: Long)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllSuppliersFlow(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: Long): Supplier?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Query("UPDATE suppliers SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteSupplier(id: Long)

    @Query("SELECT * FROM suppliers WHERE isDeleted = 1")
    fun getRecycleBinSuppliersFlow(): Flow<List<Supplier>>

    @Query("UPDATE suppliers SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreSupplier(id: Long)
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllPurchasesFlow(): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getPurchaseById(id: Long): Purchase?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase): Long

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun getItemsForPurchase(purchaseId: Long): List<PurchaseItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItem>)

    @Query("UPDATE purchases SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeletePurchase(id: Long)
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
    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY fullName ASC")
    fun getAllUsersFlow(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :username AND isActive = 1 LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET isActive = 0 WHERE id = :id")
    suspend fun deactivateUser(id: Long)
}

@Dao
interface StoreBranchDao {
    @Query("SELECT * FROM store_branches WHERE isActive = 1 ORDER BY id ASC")
    fun getAllBranchesFlow(): Flow<List<StoreBranch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: StoreBranch): Long

    @Update
    suspend fun updateBranch(branch: StoreBranch)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY checkInTime DESC")
    fun getAllAttendanceFlow(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE dateString = :date ORDER BY checkInTime DESC")
    fun getAttendanceForDateFlow(date: String): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord): Long

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog): Long
}
