package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import com.example.data.entity.StoreSettings
import com.example.data.entity.Supplier
import com.example.data.entity.SupplierLedger

import com.example.data.entity.UserAccount
import com.example.data.entity.DailyClosing
import com.example.data.entity.ActivityLog
import com.example.data.entity.StoreProfile
import com.example.data.entity.UserStorePermission
import com.example.data.entity.TenantAccount
import com.example.data.entity.SubscriptionPlan
import com.example.data.entity.RecycleBinItem
import com.example.data.entity.SuperAdminRecovery
import com.example.data.entity.AttendanceRecord

@Database(
    entities = [
        TenantAccount::class,
        SubscriptionPlan::class,
        StoreSettings::class,
        StoreProfile::class,
        UserStorePermission::class,
        CategoryEntity::class,
        Product::class,
        Customer::class,
        Supplier::class,
        Sale::class,
        SaleItem::class,
        Purchase::class,
        PurchaseItem::class,
        CustomerLedger::class,
        SupplierLedger::class,
        Expense::class,
        UserAccount::class,
        DailyClosing::class,
        ActivityLog::class,
        RecycleBinItem::class,
        SuperAdminRecovery::class,
        AttendanceRecord::class
    ],
    version = 19,
    exportSchema = false
)
abstract class StoreDatabase : RoomDatabase() {

    abstract fun storeDao(): StoreDao

    companion object {
        const val DB_NAME = "store_manager_pos.db"

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE store_settings ADD COLUMN defaultCashierName TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE store_settings ADD COLUMN defaultCashierEmployeeId TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE store_settings ADD COLUMN defaultCashierPhone TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE store_settings ADD COLUMN defaultCashierDesignation TEXT NOT NULL DEFAULT 'Cashier'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE sales ADD COLUMN cashierName TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE sales ADD COLUMN cashierId INTEGER DEFAULT NULL")
                } catch (_: Exception) {}
            }
        }

        @Volatile
        private var INSTANCE: StoreDatabase? = null

        fun getInstance(context: Context): StoreDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StoreDatabase::class.java,
                    DB_NAME
                )
                .addMigrations(MIGRATION_18_19)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.let {
                    if (it.isOpen) {
                        it.close()
                    }
                }
                INSTANCE = null
            }
        }
    }
}
