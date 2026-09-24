package com.example.data.backup

import com.example.data.entity.*
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupMetadata(
    val appName: String = "SENTRY STORE POS",
    val appVersion: String = "3.2.0",
    val databaseVersion: Int = 3,
    val backupFormatVersion: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String = "",
    val deviceInstallationId: String = "",
    val recordCounts: Map<String, Int> = emptyMap(),
    val totalRecords: Int = 0,
    val checksumSha256: String = "",
    val isEncrypted: Boolean = true
)

@JsonClass(generateAdapter = true)
data class BackupPackage(
    val metadata: BackupMetadata,
    val products: List<Product> = emptyList(),
    val sales: List<Sale> = emptyList(),
    val saleItems: List<SaleItem> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val purchases: List<Purchase> = emptyList(),
    val purchaseItems: List<PurchaseItem> = emptyList(),
    val storeSettings: StoreSettings? = null,
    val users: List<User> = emptyList(),
    val branches: List<StoreBranch> = emptyList(),
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val businessProfile: BusinessProfile? = null,
    val activityLogs: List<ActivityLog> = emptyList(),
    val paymentQrs: List<PaymentQrConfig> = emptyList(),
    val registerShifts: List<RegisterShift> = emptyList(),
    val cashMovements: List<CashMovement> = emptyList(),
    val saleReturns: List<SaleReturn> = emptyList(),
    val saleReturnItems: List<SaleReturnItem> = emptyList(),
    val stockMovements: List<StockMovement> = emptyList(),
    val fbrInvoiceRecords: List<FbrInvoiceRecord> = emptyList(),
    val invoiceSequences: List<InvoiceSequence> = emptyList()
)

enum class BackupDestination {
    GOOGLE_DRIVE,
    LOCAL_STORAGE,
    SAFETY_BACKUP
}

enum class BackupStatusType {
    SUCCESS,
    FAILED,
    IN_PROGRESS,
    QUEUED_OFFLINE
}

@JsonClass(generateAdapter = true)
data class BackupHistoryItem(
    val id: String,
    val filename: String,
    val timestamp: Long,
    val formattedDate: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val destination: BackupDestination,
    val status: BackupStatusType,
    val totalRecords: Int,
    val checksumSha256: String = "",
    val driveFileId: String? = null,
    val localFilePath: String? = null,
    val accountEmail: String? = null,
    val note: String = ""
)

enum class AutoBackupFrequency(val displayName: String) {
    OFF("Disabled"),
    DAILY("Daily (Automatic)"),
    WEEKLY("Weekly (Automatic)")
}

sealed class BackupOpState {
    object Idle : BackupOpState()
    data class InProgress(val message: String, val progressPercent: Int = -1) : BackupOpState()
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : BackupOpState()
    data class Error(val errorMessage: String, val isOffline: Boolean = false) : BackupOpState()
    data class WaitingForNetwork(val message: String = "Backup queued. Waiting for internet connection.") : BackupOpState()
}

data class GoogleAccountInfo(
    val email: String,
    val displayName: String,
    val connectedAt: Long = System.currentTimeMillis()
)
