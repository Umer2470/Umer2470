package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "store_settings")
data class StoreSettings(
    @PrimaryKey val id: Int = 1,
    val tenantId: String = "TENANT_DEFAULT",
    val storeName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val whatsappNumber: String = "",
    val address: String = "",
    val businessType: String = "",
    val pinCode: String = "1234",
    val isPinEnabled: Boolean = true,
    val isBiometricEnabled: Boolean = true,
    val currencySymbol: String = "Rs.",
    val taxPercentage: Double = 0.0,
    val defaultLowStockThreshold: Double = 5.0,
    val themeMode: String = "SYSTEM",
    val accentColor: String = "BLUE",
    val dashboardWidgetOrder: String = "QUICK_SALE,ANALYTICS_CHARTS,LOW_STOCK,TODAY_SALES_KPI,STORE_OPERATIONS,RECENT_TRANSACTIONS",
    val logoUri: String = "",
    val activeStoreId: Long = 1L,
    val isOnboardingCompleted: Boolean = false,
    val isBarcodeEnabled: Boolean = true,
    val isQrCodeEnabled: Boolean = true,
    val isWhatsappInvoiceEnabled: Boolean = true,
    val isThermalPrinterEnabled: Boolean = true,
    val isCreditSaleEnabled: Boolean = true,
    val isPurchaseReturnEnabled: Boolean = true,
    val isSalesReturnEnabled: Boolean = true,
    val isExpenseModuleEnabled: Boolean = true,
    val isCashBookEnabled: Boolean = true,
    val isAttendanceEnabled: Boolean = true,
    val isEmployeeCommissionEnabled: Boolean = true,
    val invoiceHeader: String = "Thank you for shopping with us!",
    val invoiceFooter: String = "Software Powered by Al-Khair Store Manager & POS",
    val defaultCashierName: String = "",
    val defaultCashierEmployeeId: String = "",
    val defaultCashierPhone: String = "",
    val defaultCashierDesignation: String = "Cashier"
)

@Entity(
    tableName = "stores",
    indices = [
        Index("tenantId"),
        Index("code")
    ]
)
data class StoreProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val storeName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val whatsappNumber: String = "",
    val address: String = "",
    val businessType: String = "",
    val logoUri: String = "",
    val code: String = "MAIN",
    val isPrimary: Boolean = true,
    val secretCode: String = "",
    val qrCode: String = "",
    val secretCodeHash: String = "",
    val qrCodeHash: String = "",
    val isAccessCodeEnabled: Boolean = true,
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_store_permissions",
    indices = [
        Index("tenantId"),
        Index("userId"),
        Index("storeId")
    ]
)
data class UserStorePermission(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val userId: Long,
    val storeId: Long,
    val verifiedCodeHash: String = "",
    val grantedBy: String = "SECRET_CODE", // "SECRET_CODE", "QR_CODE", "SUPER_ADMIN"
    val isGranted: Boolean = true,
    val grantedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "categories",
    indices = [
        Index("tenantId"),
        Index("name")
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val name: String,
    val description: String = ""
)

@Entity(
    tableName = "products",
    indices = [
        Index("tenantId"),
        Index("storeId"),
        Index("barcode"),
        Index("name"),
        Index("category")
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val storeId: Long = 1L,
    val barcode: String = "",
    val name: String,
    val category: String,
    val purchasePrice: Double,
    val salePrice: Double,
    val stockQuantity: Double,
    val minStockLevel: Double = 5.0,
    val unit: String = "Pcs",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customers",
    indices = [
        Index("tenantId"),
        Index("storeId"),
        Index("name"),
        Index("phone")
    ]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val storeId: Long = 1L,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val balance: Double = 0.0, // Positive = customer owes store (udhaar)
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "suppliers",
    indices = [
        Index("tenantId"),
        Index("storeId"),
        Index("name")
    ]
)
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val storeId: Long = 1L,
    val name: String,
    val company: String = "",
    val phone: String = "",
    val address: String = "",
    val payableBalance: Double = 0.0, // Positive = store owes supplier
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sales",
    indices = [
        Index("tenantId"),
        Index("storeId"),
        Index("timestamp"),
        Index("invoiceNumber"),
        Index("customerId")
    ]
)
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val storeId: Long = 1L,
    val invoiceNumber: String,
    val customerId: Long? = null,
    val customerName: String = "Cash Customer",
    val totalAmount: Double,
    val discount: Double = 0.0,
    val netAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,
    val paymentType: String = "Cash", // Cash, Udhaar / Credit, Card
    val cashierName: String = "", // Cashier / Operator recorded for this specific invoice
    val cashierId: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sale_items",
    indices = [
        Index("saleId"),
        Index("productId")
    ]
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unit: String = "Pcs",
    val purchasePrice: Double,
    val salePrice: Double,
    val totalPrice: Double
)

@Entity(
    tableName = "purchases",
    indices = [
        Index("tenantId"),
        Index("storeId"),
        Index("timestamp"),
        Index("supplierId")
    ]
)
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val storeId: Long = 1L,
    val purchaseNumber: String,
    val supplierId: Long? = null,
    val supplierName: String = "General Vendor",
    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_items",
    indices = [
        Index("purchaseId"),
        Index("productId")
    ]
)
data class PurchaseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val costPrice: Double,
    val totalPrice: Double
)

@Entity(
    tableName = "customer_ledgers",
    indices = [
        Index("tenantId"),
        Index("storeId"),
        Index("customerId"),
        Index("timestamp")
    ]
)
data class CustomerLedger(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val storeId: Long = 1L,
    val customerId: Long,
    val description: String,
    val type: String, // "CREDIT_SALE", "PAYMENT_RECEIVED", "DEBIT_ADJUSTMENT"
    val amount: Double,
    val balanceAfter: Double,
    val paymentMethod: String = "Cash",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "supplier_ledgers",
    indices = [
        Index("tenantId"),
        Index("storeId"),
        Index("supplierId"),
        Index("timestamp")
    ]
)
data class SupplierLedger(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val storeId: Long = 1L,
    val supplierId: Long,
    val description: String,
    val type: String, // "PURCHASE", "PAYMENT_MADE", "CREDIT_ADJUSTMENT"
    val amount: Double,
    val balanceAfter: Double,
    val paymentMethod: String = "Cash",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "expenses",
    indices = [
        Index("tenantId"),
        Index("storeId"),
        Index("timestamp"),
        Index("category")
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val storeId: Long = 1L,
    val title: String,
    val category: String = "General",
    val amount: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "app_users",
    indices = [
        Index("tenantId"),
        Index("username"),
        Index("assignedStoreId")
    ]
)
data class UserAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val username: String,
    val name: String,
    val pinCode: String,
    val role: String, // "MASTER_OWNER", "SUPER_ADMIN", "ADMIN", "EMPLOYEE"
    val phone: String = "",
    val assignedStoreId: Long = 1L, // Default primary store ID
    val assignedStoreIdsCsv: String = "", // Comma-separated assigned store IDs e.g. "1,2", or empty for Super Admin
    val lastSelectedStoreId: Long = 0L, // Store selection saved separately for this user
    val isActive: Boolean = true,
    val dailyWageRate: Double = 0.0,
    val hourlyWageRate: Double = 0.0,
    val monthlyBaseSalary: Double = 0.0,
    val overtimeHourlyRate: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getAssignedStoreIdsList(): List<Long> {
        if (role == "SUPER_ADMIN" || role == "MASTER_OWNER") return emptyList()
        val csv = assignedStoreIdsCsv.trim()
        if (csv.isNotBlank() && csv != "0") {
            val list = csv.split(",").mapNotNull { it.trim().toLongOrNull() }.filter { it > 0L }
            if (list.isNotEmpty()) return list
        }
        return if (assignedStoreId > 0L) listOf(assignedStoreId) else listOf(1L)
    }

    fun canAccessStore(storeId: Long): Boolean {
        if (role == "SUPER_ADMIN" || role == "MASTER_OWNER") return true
        if (storeId == 0L) return false // Only Super Admin/Master Owner can access Combined View
        val allowedList = getAssignedStoreIdsList()
        return allowedList.contains(storeId)
    }
}

@Entity(
    tableName = "daily_closings",
    indices = [
        Index("tenantId"),
        Index("storeId"),
        Index("openTimestamp"),
        Index("closeTimestamp")
    ]
)
data class DailyClosing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val storeId: Long = 1L,
    val userId: Long,
    val userName: String,
    val userRole: String,
    val openingCash: Double,
    val closingCash: Double,
    val totalSales: Double = 0.0,
    val cashSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val returnedProductsAmount: Double = 0.0,
    val expectedCash: Double = 0.0,
    val differenceAmount: Double = 0.0,
    val status: String = "CLOSED", // "OPEN", "CLOSED"
    val openTimestamp: Long = System.currentTimeMillis(),
    val closeTimestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(
    tableName = "activity_logs",
    indices = [
        Index("tenantId"),
        Index("storeId"),
        Index("timestamp")
    ]
)
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val storeId: Long = 1L,
    val userId: Long = 0,
    val userName: String,
    val userRole: String,
    val action: String,
    val details: String = "",
    val device: String = "Android Device",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "recycle_bin",
    indices = [
        Index("tenantId"),
        Index("storeId"),
        Index("itemType"),
        Index("deletedAt")
    ]
)
data class RecycleBinItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val storeId: Long = 1L,
    val itemType: String, // "PRODUCT", "CUSTOMER", "SUPPLIER", "SALE", "PURCHASE"
    val originalId: Long,
    val title: String,
    val subtitle: String = "",
    val jsonData: String = "",
    val deletedBy: String = "Super Admin",
    val deletedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance_records",
    indices = [
        Index("tenantId"),
        Index("storeId"),
        Index("userId"),
        Index("dateStr"),
        Index("timestamp")
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "TENANT_DEFAULT",
    val storeId: Long = 1L,
    val userId: Long,
    val userName: String,
    val userRole: String = "EMPLOYEE",
    val dateStr: String, // "YYYY-MM-DD"
    val status: String = "PRESENT", // "PRESENT", "ABSENT", "LEAVE", "LATE"
    val checkInTime: Long? = null,
    val checkOutTime: Long? = null,
    val breakStartTime: Long? = null,
    val breakEndTime: Long? = null,
    val totalWorkingMinutes: Long = 0,
    val overtimeMinutes: Long = 0,
    val dailyGrossPay: Double = 0.0,
    val overtimePay: Double = 0.0,
    val totalDailyPay: Double = 0.0,
    val deductions: Double = 0.0,
    val allowances: Double = 0.0,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)


