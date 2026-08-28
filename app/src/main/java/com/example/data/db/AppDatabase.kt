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
                    storeName = "CH UMER POS.03080018035",
                    appDisplayName = "VIP POS",
                    posBrandName = "VIP POS",
                    tagline = "SMART | FAST | RELIABLE",
                    brandDescription = "ALL-IN-ONE BUSINESS SOLUTION",
                    logoUri = null,
                    ownerName = "CH UMER",
                    phone = "03080018035",
                    address = "Main Market, Store #1",
                    currencySymbol = "Rs"
                )
            )
            database.businessProfileDao().insertOrUpdateProfile(
                BusinessProfile(
                    id = 1,
                    businessName = "CH UMER POS.03080018035",
                    supportPhone = "03080018035"
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
        }
    }
}
