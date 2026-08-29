package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Product::class,
        Sale::class,
        SaleItem::class,
        Customer::class,
        Supplier::class,
        Purchase::class,
        PurchaseItem::class,
        StoreSettings::class,
        User::class,
        StoreBranch::class,
        AttendanceRecord::class,
        BusinessProfile::class,
        ActivityLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun storeSettingsDao(): StoreSettingsDao
    abstract fun userDao(): UserDao
    abstract fun storeBranchDao(): StoreBranchDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE store_settings ADD COLUMN appDisplayName TEXT NOT NULL DEFAULT 'VIP POS'")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN posBrandName TEXT NOT NULL DEFAULT 'VIP POS'")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN tagline TEXT NOT NULL DEFAULT 'SMART | FAST | RELIABLE'")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN brandDescription TEXT NOT NULL DEFAULT 'ALL-IN-ONE BUSINESS SOLUTION'")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN logoUri TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ch_umer_pos_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(false)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    seedInitialData(database)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(database: AppDatabase) {
            database.storeSettingsDao().insertOrUpdateSettings(
                StoreSettings(
                    id = 1,
                    storeName = "SENTRY STORE",
                    appDisplayName = "SENTRY STORE",
                    posBrandName = "SENTRY STORE POS",
                    tagline = "Professional Retail & Business Management",
                    brandDescription = "Hardware, Paint & Multi-Category Retail POS",
                    logoUri = null,
                    ownerName = "CH UMER",
                    phone = "03080018035",
                    email = "sentrystore.pk@gmail.com",
                    address = "Main Market, Store #1",
                    currencySymbol = "Rs",
                    invoiceFooterText = "Thank you for shopping with SENTRY STORE! No return without receipt.",
                    defaultCashierName = "Muhammad Umer"
                )
            )
            database.businessProfileDao().insertOrUpdateProfile(
                BusinessProfile(
                    id = 1,
                    businessName = "SENTRY STORE",
                    tagline = "Professional Retail & Business Management",
                    supportPhone = "03080018035",
                    supportEmail = "sentrystore.pk@gmail.com",
                    website = "https://sentrystore.pk"
                )
            )
            database.storeBranchDao().insertBranch(
                StoreBranch(
                    id = 1,
                    name = "Head Office Branch",
                    location = "Main Market",
                    isHeadquarters = true
                )
            )
            database.userDao().insertUser(
                User(
                    id = 1,
                    username = "admin",
                    pinHash = "1234",
                    role = "Admin",
                    fullName = "Administrator"
                )
            )
            database.userDao().insertUser(
                User(
                    id = 2,
                    username = "umer",
                    pinHash = "1122",
                    role = "Cashier",
                    fullName = "Muhammad Umer",
                    phone = "03080018035"
                )
            )
            // Seed initial sample inventory for Paint & Hardware
            val sampleProducts = listOf(
                Product(name = "Berger WeatherCoat Emulsion (Off-White)", category = "Paints", barcode = "8901234001", purchasePrice = 3200.0, salePrice = 3850.0, stockQuantity = 24.0, unit = "Gallon", minStockAlert = 5.0),
                Product(name = "Master Synthetic Enamel Paint (Gloss White)", category = "Paints", barcode = "8901234002", purchasePrice = 950.0, salePrice = 1200.0, stockQuantity = 40.0, unit = "Litre", minStockAlert = 8.0),
                Product(name = "Diamond Matt Finish Paint (Soft Grey)", category = "Paints", barcode = "8901234003", purchasePrice = 8500.0, salePrice = 9900.0, stockQuantity = 12.0, unit = "Drum", minStockAlert = 3.0),
                Product(name = "Falcon Portland Cement Grade 43", category = "Building Material", barcode = "8901234004", purchasePrice = 1180.0, salePrice = 1320.0, stockQuantity = 150.0, unit = "Bag", minStockAlert = 20.0),
                Product(name = "Deformed Steel Rebar 1/2 Inch (Grade 60)", category = "Building Material", barcode = "8901234005", purchasePrice = 240.0, salePrice = 275.0, stockQuantity = 500.0, unit = "Kg", minStockAlert = 50.0),
                Product(name = "Heavy Duty Brass Ball Valve 1/2\"", category = "Hardware", barcode = "8901234006", purchasePrice = 450.0, salePrice = 650.0, stockQuantity = 60.0, unit = "Pcs", minStockAlert = 10.0),
                Product(name = "Professional 4-Inch Paint Roller & Tray Set", category = "Paint Tools", barcode = "8901234007", purchasePrice = 380.0, salePrice = 550.0, stockQuantity = 35.0, unit = "Set", minStockAlert = 5.0),
                Product(name = "PPRC Pipe PN-20 High Pressure (25mm)", category = "Sanitary & Pipes", barcode = "8901234008", purchasePrice = 320.0, salePrice = 420.0, stockQuantity = 80.0, unit = "Meter", minStockAlert = 15.0)
            )
            sampleProducts.forEach { database.productDao().insertProduct(it) }
        }
    }
}
