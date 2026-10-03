package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        CustomerEntity::class,
        SupplierEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        PaymentRecordEntity::class,
        CustomerLedgerEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao
    abstract fun paymentRecordDao(): PaymentRecordDao
    abstract fun customerLedgerDao(): CustomerLedgerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pos_store_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                seedInitialData(getDatabase(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(db: AppDatabase) {
            val productDao = db.productDao()
            val customerDao = db.customerDao()

            if (productDao.getAllProducts(1).isEmpty()) {
                val initialProducts = listOf(
                    ProductEntity(name = "Super Basmati Rice 1 Kg", barcode = "8901234567890", category = "Grocery", purchasePrice = 210.0, salePrice = 260.0, stockQuantity = 85.0, unit = "Kg"),
                    ProductEntity(name = "Dalda Cooking Oil 1 Litre", barcode = "8909876543210", category = "Grocery", purchasePrice = 450.0, salePrice = 520.0, stockQuantity = 42.0, unit = "Bottle"),
                    ProductEntity(name = "Refined Sugar 1 Kg", barcode = "2000000000017", category = "Grocery", purchasePrice = 120.0, salePrice = 145.0, stockQuantity = 150.0, unit = "Kg"),
                    ProductEntity(name = "Nestle MilkPak 1000ml", barcode = "8904561237891", category = "Dairy", purchasePrice = 240.0, salePrice = 280.0, stockQuantity = 60.0, unit = "Pack"),
                    ProductEntity(name = "Tapal Danedar Tea 400g", barcode = "8906549873215", category = "Beverages", purchasePrice = 480.0, salePrice = 560.0, stockQuantity = 35.0, unit = "Box")
                )
                productDao.insertProducts(initialProducts)
            }

            if (customerDao.getAllCustomers(1).isEmpty()) {
                customerDao.insertCustomer(CustomerEntity(name = "Muhammad Ali", phone = "0300-1122334", address = "Model Town, Lahore", currentBalance = 0.0))
                customerDao.insertCustomer(CustomerEntity(name = "Tariq Mahmood", phone = "0321-9988776", address = "Gulberg III, Lahore", currentBalance = 1500.0))
            }
        }
    }
}
