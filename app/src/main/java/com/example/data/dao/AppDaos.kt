package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE storeId = :storeId ORDER BY id DESC")
    fun getAllProductsFlow(storeId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE storeId = :storeId ORDER BY id DESC")
    suspend fun getAllProducts(storeId: Long): List<ProductEntity>

    @Query("SELECT * FROM products WHERE storeId = :storeId AND (name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%')")
    fun searchProductsFlow(query: String, storeId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE barcode = :barcode AND storeId = :storeId LIMIT 1")
    suspend fun getProductByBarcode(barcode: String, storeId: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>): List<Long>

    @Update
    suspend fun updateProduct(product: ProductEntity): Int

    @Delete
    suspend fun deleteProduct(product: ProductEntity): Int

    @Query("UPDATE products SET stockQuantity = stockQuantity - :qty WHERE id = :id")
    suspend fun decrementStock(id: Long, qty: Double): Int
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE storeId = :storeId ORDER BY name ASC")
    fun getAllCustomersFlow(storeId: Long): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE storeId = :storeId ORDER BY name ASC")
    suspend fun getAllCustomers(storeId: Long): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity): Int

    @Query("UPDATE customers SET currentBalance = currentBalance + :amountDebt WHERE id = :id")
    suspend fun addDebt(id: Long, amountDebt: Double): Int

    @Query("UPDATE customers SET currentBalance = currentBalance - :amountCredit WHERE id = :id")
    suspend fun applyPaymentCredit(id: Long, amountCredit: Double): Int
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE storeId = :storeId ORDER BY name ASC")
    fun getAllSuppliersFlow(storeId: Long): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE storeId = :storeId ORDER BY id DESC")
    fun getAllInvoicesFlow(storeId: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE storeId = :storeId ORDER BY id DESC")
    suspend fun getAllInvoices(storeId: Long): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: Long): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE invoiceNo = :invoiceNo LIMIT 1")
    suspend fun getInvoiceByNo(invoiceNo: String): InvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Query("UPDATE invoices SET amountApplied = amountApplied + :paymentAmount, balanceDue = MAX(0.0, balanceDue - :paymentAmount), paymentStatus = CASE WHEN (balanceDue - :paymentAmount) <= 0.001 THEN 'PAID' ELSE 'PARTIALLY_PAID' END WHERE id = :invoiceId")
    suspend fun applySettlementPayment(invoiceId: Long, paymentAmount: Double): Int

    @Query("SELECT COUNT(*) FROM invoices WHERE storeId = :storeId")
    suspend fun getInvoiceCount(storeId: Long): Long
}

@Dao
interface InvoiceItemDao {
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getItemsForInvoice(invoiceId: Long): List<InvoiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InvoiceItemEntity>): List<Long>
}

@Dao
interface PaymentRecordDao {
    @Query("SELECT * FROM payment_records WHERE invoiceId = :invoiceId ORDER BY id ASC")
    suspend fun getPaymentsForInvoice(invoiceId: Long): List<PaymentRecordEntity>

    @Query("SELECT COUNT(*) FROM payment_records WHERE idempotencyKey = :key AND :key != ''")
    suspend fun checkDuplicateKey(key: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(record: PaymentRecordEntity): Long
}

@Dao
interface CustomerLedgerDao {
    @Query("SELECT * FROM customer_ledger WHERE customerId = :customerId ORDER BY id DESC")
    fun getLedgerFlow(customerId: Long): Flow<List<CustomerLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CustomerLedgerEntity): Long
}
