package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import com.example.util.SecurityUtils
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
        ActivityLog::class,
        PaymentQrConfig::class,
        RegisterShift::class,
        CashMovement::class,
        SaleReturn::class,
        SaleReturnItem::class,
        StockMovement::class,
        FbrInvoiceRecord::class,
        EmployeeSalaryConfig::class,
        PayrollRecord::class,
        AttendanceMachineConfig::class,
        MachinePunchLog::class,
        InvoiceSequence::class
    ],
    version = 8,
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
    abstract fun employeeSalaryConfigDao(): EmployeeSalaryConfigDao
    abstract fun payrollDao(): PayrollDao
    abstract fun attendanceMachineConfigDao(): AttendanceMachineConfigDao
    abstract fun machinePunchLogDao(): MachinePunchLogDao
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun paymentQrConfigDao(): PaymentQrConfigDao
    abstract fun registerShiftDao(): RegisterShiftDao
    abstract fun cashMovementDao(): CashMovementDao
    abstract fun saleReturnDao(): SaleReturnDao
    abstract fun saleReturnItemDao(): SaleReturnItemDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun fbrInvoiceRecordDao(): FbrInvoiceRecordDao
    abstract fun invoiceSequenceDao(): InvoiceSequenceDao


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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `payment_qr_configs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `paymentType` TEXT NOT NULL,
                        `imagePath` TEXT NOT NULL,
                        `accountTitle` TEXT NOT NULL,
                        `accountNumber` TEXT NOT NULL,
                        `instructions` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL DEFAULT 1,
                        `isDefault` INTEGER NOT NULL DEFAULT 0,
                        `displayOrder` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("ALTER TABLE store_settings ADD COLUMN isScanToPayEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN scanToPayLabel TEXT NOT NULL DEFAULT 'SCAN TO PAY'")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN activePaymentQrId INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `register_shifts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `shiftNumber` TEXT NOT NULL,
                        `cashierName` TEXT NOT NULL,
                        `openedAt` INTEGER NOT NULL,
                        `closedAt` INTEGER,
                        `openingCash` REAL NOT NULL,
                        `openingNotes` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `cashSales` REAL NOT NULL,
                        `cardSales` REAL NOT NULL,
                        `creditSales` REAL NOT NULL,
                        `totalSales` REAL NOT NULL,
                        `cashIn` REAL NOT NULL,
                        `cashOut` REAL NOT NULL,
                        `expectedCash` REAL NOT NULL,
                        `actualCash` REAL NOT NULL,
                        `discrepancy` REAL NOT NULL,
                        `closingNotes` TEXT NOT NULL,
                        `totalInvoices` INTEGER NOT NULL,
                        `branchId` INTEGER NOT NULL,
                        `denomination5000` INTEGER NOT NULL,
                        `denomination1000` INTEGER NOT NULL,
                        `denomination500` INTEGER NOT NULL,
                        `denomination100` INTEGER NOT NULL,
                        `denomination50` INTEGER NOT NULL,
                        `denomination20` INTEGER NOT NULL,
                        `denomination10` INTEGER NOT NULL,
                        `denominationCoins` REAL NOT NULL,
                        `closedBy` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cash_movements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `shiftId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `reason` TEXT NOT NULL,
                        `cashierName` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sale_returns` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `returnNumber` TEXT NOT NULL,
                        `saleId` INTEGER NOT NULL,
                        `originalInvoiceNumber` TEXT NOT NULL,
                        `customerId` INTEGER NOT NULL,
                        `customerName` TEXT NOT NULL,
                        `refundAmount` REAL NOT NULL,
                        `refundPaymentType` TEXT NOT NULL,
                        `taxRefundAmount` REAL NOT NULL,
                        `reason` TEXT NOT NULL,
                        `processedBy` TEXT NOT NULL,
                        `branchId` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sale_return_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `returnId` INTEGER NOT NULL,
                        `saleItemId` INTEGER NOT NULL,
                        `productId` INTEGER NOT NULL,
                        `productName` TEXT NOT NULL,
                        `quantityReturned` REAL NOT NULL,
                        `unit` TEXT NOT NULL,
                        `unitPrice` REAL NOT NULL,
                        `purchasePrice` REAL NOT NULL,
                        `totalRefund` REAL NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stock_movements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `productId` INTEGER NOT NULL,
                        `productName` TEXT NOT NULL,
                        `quantityDelta` REAL NOT NULL,
                        `stockBefore` REAL NOT NULL,
                        `stockAfter` REAL NOT NULL,
                        `movementType` TEXT NOT NULL,
                        `referenceId` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `performedBy` TEXT NOT NULL,
                        `branchId` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `fbr_invoice_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `saleId` INTEGER NOT NULL,
                        `invoiceNumber` TEXT NOT NULL,
                        `usin` TEXT NOT NULL,
                        `posId` TEXT NOT NULL,
                        `ntn` TEXT NOT NULL,
                        `strn` TEXT NOT NULL,
                        `totalTaxableAmount` REAL NOT NULL,
                        `totalTaxAmount` REAL NOT NULL,
                        `invoiceGrandTotal` REAL NOT NULL,
                        `submissionStatus` TEXT NOT NULL,
                        `fbrInvoiceNumber` TEXT NOT NULL,
                        `qrCodeData` TEXT NOT NULL,
                        `responseCode` TEXT NOT NULL,
                        `responseMessage` TEXT NOT NULL,
                        `submittedAt` INTEGER,
                        `retryCount` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                // StoreSettings FBR updates
                db.execSQL("ALTER TABLE store_settings ADD COLUMN isFbrIntegrationEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN fbrPosId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN fbrNtn TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN fbrStrn TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN fbrBusinessName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN fbrEnvironment TEXT NOT NULL DEFAULT 'Sandbox'")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN fbrApiAuthToken TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN fbrDefaultTaxRate REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN fbrTaxMode TEXT NOT NULL DEFAULT 'Exclusive'")

                // Product expiry, batch, multi-unit, and tax updates
                db.execSQL("ALTER TABLE products ADD COLUMN expiryDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE products ADD COLUMN batchNumber TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE products ADD COLUMN secondaryUnit TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE products ADD COLUMN unitConversionRate REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE products ADD COLUMN isTaxExempt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE products ADD COLUMN customTaxRate REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Update attendance_records
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN designation TEXT NOT NULL DEFAULT 'Staff'")
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN workingHours REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN machineLogId TEXT NOT NULL DEFAULT ''")

                // Create employee_salary_configs
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `employee_salary_configs` (
                        `employeeId` INTEGER PRIMARY KEY NOT NULL,
                        `employeeName` TEXT NOT NULL DEFAULT '',
                        `designation` TEXT NOT NULL DEFAULT 'Staff',
                        `basicSalary` REAL NOT NULL DEFAULT 0.0,
                        `monthlyAllowances` REAL NOT NULL DEFAULT 0.0,
                        `overtimeHourlyRate` REAL NOT NULL DEFAULT 0.0,
                        `lateDeductionPerDay` REAL NOT NULL DEFAULT 0.0,
                        `absentDeductionPerDay` REAL NOT NULL DEFAULT 0.0,
                        `enableLateDeduction` INTEGER NOT NULL DEFAULT 0,
                        `enableAbsentDeduction` INTEGER NOT NULL DEFAULT 0,
                        `joiningDate` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Create payroll_records
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `payroll_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `employeeId` INTEGER NOT NULL DEFAULT 0,
                        `employeeName` TEXT NOT NULL DEFAULT '',
                        `designation` TEXT NOT NULL DEFAULT 'Staff',
                        `monthYear` TEXT NOT NULL DEFAULT '',
                        `basicSalary` REAL NOT NULL DEFAULT 0.0,
                        `workingDays` INTEGER NOT NULL DEFAULT 26,
                        `presentDays` INTEGER NOT NULL DEFAULT 0,
                        `absentDays` INTEGER NOT NULL DEFAULT 0,
                        `leaveDays` INTEGER NOT NULL DEFAULT 0,
                        `lateDays` INTEGER NOT NULL DEFAULT 0,
                        `halfDays` INTEGER NOT NULL DEFAULT 0,
                        `overtimeHours` REAL NOT NULL DEFAULT 0.0,
                        `overtimeAmount` REAL NOT NULL DEFAULT 0.0,
                        `allowances` REAL NOT NULL DEFAULT 0.0,
                        `deductions` REAL NOT NULL DEFAULT 0.0,
                        `deductionReason` TEXT NOT NULL DEFAULT '',
                        `grossSalary` REAL NOT NULL DEFAULT 0.0,
                        `netSalary` REAL NOT NULL DEFAULT 0.0,
                        `paidAmount` REAL NOT NULL DEFAULT 0.0,
                        `paymentStatus` TEXT NOT NULL DEFAULT 'PENDING',
                        `paymentDate` INTEGER NOT NULL DEFAULT 0,
                        `paymentMethod` TEXT NOT NULL DEFAULT 'Cash',
                        `paymentReference` TEXT NOT NULL DEFAULT '',
                        `authorizedBy` TEXT NOT NULL DEFAULT '',
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Create attendance_machine_configs
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `attendance_machine_configs` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `deviceName` TEXT NOT NULL DEFAULT 'ZKTeco K40 Biometric',
                        `deviceId` TEXT NOT NULL DEFAULT 'DEV-ZK-101',
                        `connectionType` TEXT NOT NULL DEFAULT 'TCP/IP Network',
                        `ipAddress` TEXT NOT NULL DEFAULT '192.168.1.201',
                        `port` INTEGER NOT NULL DEFAULT 4370,
                        `isAutoSyncEnabled` INTEGER NOT NULL DEFAULT 0,
                        `isConnected` INTEGER NOT NULL DEFAULT 0,
                        `lastSyncTime` INTEGER NOT NULL DEFAULT 0,
                        `lastSuccessfulSyncTime` INTEGER NOT NULL DEFAULT 0,
                        `syncStatus` TEXT NOT NULL DEFAULT 'NOT CONNECTED'
                    )
                """.trimIndent())

                // Create machine_punch_logs
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `machine_punch_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `machineRecordKey` TEXT NOT NULL DEFAULT '',
                        `employeeId` INTEGER NOT NULL DEFAULT 0,
                        `employeeName` TEXT NOT NULL DEFAULT '',
                        `punchTime` INTEGER NOT NULL DEFAULT 0,
                        `punchType` TEXT NOT NULL DEFAULT 'Check-In',
                        `syncedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_machine_punch_logs_machineRecordKey` ON `machine_punch_logs` (`machineRecordKey`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `invoice_sequences` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `lastSerial` INTEGER NOT NULL DEFAULT 0,
                        `prefix` TEXT NOT NULL DEFAULT 'INV.',
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE store_settings ADD COLUMN dashboardBannerUri TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN dashboardSmallImageUri TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN dashboardBannerBgColor TEXT NOT NULL DEFAULT 'Navy'")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN showDashboardBannerText INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN showDashboardSmallImage INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN dashboardBannerHeading TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN dashboardBannerSubtitle TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN dashboardBannerDescription TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE store_settings ADD COLUMN dashboardBannerActionText TEXT NOT NULL DEFAULT 'High-Speed Billing & Inventory'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sentry_store_pos_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
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
            if (database.invoiceSequenceDao().getSequence(1) == null) {
                database.invoiceSequenceDao().insertOrUpdate(
                    InvoiceSequence(
                        id = 1,
                        lastSerial = 0L,
                        prefix = "INV.",
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
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
                    username = "superadmin",
                    pinHash = SecurityUtils.sha256("2026"),
                    role = "SUPER_ADMIN",
                    fullName = "Super Administrator"
                )
            )
            database.userDao().insertUser(
                User(
                    id = 2,
                    username = "admin",
                    pinHash = SecurityUtils.sha256("8888"),
                    role = "ADMIN",
                    fullName = "Store Administrator"
                )
            )
            database.userDao().insertUser(
                User(
                    id = 3,
                    username = "supervisor",
                    pinHash = SecurityUtils.sha256("5555"),
                    role = "SUPERVISOR",
                    fullName = "Store Supervisor"
                )
            )
            database.userDao().insertUser(
                User(
                    id = 4,
                    username = "cashier",
                    pinHash = SecurityUtils.sha256("1111"),
                    role = "CASHIER",
                    fullName = "Store Cashier",
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
