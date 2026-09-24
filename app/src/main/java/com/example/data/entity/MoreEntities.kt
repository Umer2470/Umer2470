package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "store_settings")
data class StoreSettings(
    @PrimaryKey
    val id: Long = 1,
    val storeName: String = "SENTRY STORE",
    val appDisplayName: String = "SENTRY STORE",
    val posBrandName: String = "SENTRY STORE POS",
    val tagline: String = "Professional Retail & Business Management",
    val brandDescription: String = "Hardware, Paint & Multi-Category Retail POS",
    val logoUri: String? = null,
    val ownerName: String = "CH UMER",
    val phone: String = "03080018035",
    val email: String = "sentrystore.pk@gmail.com",
    val address: String = "Main Market, Store #1",
    val currencySymbol: String = "Rs",
    val invoiceFooterText: String = "Thank you for shopping with SENTRY STORE! No return without receipt.",
    val taxRatePercent: Double = 0.0,
    val defaultCashierName: String = "Muhammad Umer",
    val defaultCashierDesignation: String = "Head Cashier",
    val isMultiBranchEnabled: Boolean = true,
    val activeBranchId: Long = 1,
    val enableCloudBackup: Boolean = true,
    val enableSoundEffects: Boolean = true,
    val paperWidthMm: Int = 80, // 58mm or 80mm
    val isScanToPayEnabled: Boolean = false,
    val scanToPayLabel: String = "SCAN TO PAY",
    val activePaymentQrId: Long? = null,
    val isFbrIntegrationEnabled: Boolean = false,
    val fbrPosId: String = "",
    val fbrNtn: String = "",
    val fbrStrn: String = "",
    val fbrBusinessName: String = "",
    val fbrEnvironment: String = "Sandbox", // Sandbox or Live
    val fbrApiAuthToken: String = "",
    val fbrDefaultTaxRate: Double = 0.0,
    val fbrTaxMode: String = "Exclusive" // Exclusive or Inclusive
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String = "",
    val pinHash: String = "",
    val role: String = "Admin", // Admin, Cashier, Manager
    val fullName: String = "",
    val phone: String = "",
    val branchId: Long = 1,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "store_branches")
data class StoreBranch(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "Main Branch",
    val location: String = "HQ",
    val phone: String = "",
    val managerName: String = "",
    val isHeadquarters: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: Long = 0,
    val employeeName: String = "",
    val designation: String = "Staff",
    val dateString: String = "", // yyyy-MM-dd
    val checkInTime: Long = 0,
    val checkOutTime: Long = 0,
    val workingHours: Double = 0.0,
    val status: String = "Present", // Present, Late, Absent, Half-Day, Leave, Off Day
    val branchId: Long = 1,
    val notes: String = "",
    val machineLogId: String = ""
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "employee_salary_configs")
data class EmployeeSalaryConfig(
    @PrimaryKey
    val employeeId: Long = 0,
    val employeeName: String = "",
    val designation: String = "Staff",
    val basicSalary: Double = 0.0,
    val monthlyAllowances: Double = 0.0,
    val overtimeHourlyRate: Double = 0.0,
    val lateDeductionPerDay: Double = 0.0,
    val absentDeductionPerDay: Double = 0.0,
    val enableLateDeduction: Boolean = false,
    val enableAbsentDeduction: Boolean = false,
    val joiningDate: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "payroll_records")
data class PayrollRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: Long = 0,
    val employeeName: String = "",
    val designation: String = "Staff",
    val monthYear: String = "", // yyyy-MM
    val basicSalary: Double = 0.0,
    val workingDays: Int = 26,
    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val leaveDays: Int = 0,
    val lateDays: Int = 0,
    val halfDays: Int = 0,
    val overtimeHours: Double = 0.0,
    val overtimeAmount: Double = 0.0,
    val allowances: Double = 0.0,
    val deductions: Double = 0.0,
    val deductionReason: String = "",
    val grossSalary: Double = 0.0,
    val netSalary: Double = 0.0,
    val paidAmount: Double = 0.0,
    val paymentStatus: String = "PENDING", // PENDING, PAID, PARTIALLY PAID
    val paymentDate: Long = 0,
    val paymentMethod: String = "Cash", // Cash, Bank Transfer, Cheque
    val paymentReference: String = "",
    val authorizedBy: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "attendance_machine_configs")
data class AttendanceMachineConfig(
    @PrimaryKey
    val id: Long = 1,
    val deviceName: String = "ZKTeco K40 Biometric",
    val deviceId: String = "DEV-ZK-101",
    val connectionType: String = "TCP/IP Network", // TCP/IP Network, USB, RS485, Cloud API
    val ipAddress: String = "192.168.1.201",
    val port: Int = 4370,
    val isAutoSyncEnabled: Boolean = false,
    val isConnected: Boolean = false,
    val lastSyncTime: Long = 0,
    val lastSuccessfulSyncTime: Long = 0,
    val syncStatus: String = "NOT CONNECTED"
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "machine_punch_logs",
    indices = [Index(value = ["machineRecordKey"], unique = true)]
)
data class MachinePunchLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val machineRecordKey: String = "", // Unique key to guarantee idempotent sync
    val employeeId: Long = 0,
    val employeeName: String = "",
    val punchTime: Long = 0,
    val punchType: String = "Check-In", // Check-In, Check-Out
    val syncedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "business_profiles")
data class BusinessProfile(
    @PrimaryKey
    val id: Long = 1,
    val businessName: String = "SENTRY STORE",
    val tagline: String = "Professional Retail & Business Management",
    val taxNumber: String = "",
    val registrationNumber: String = "",
    val supportPhone: String = "03080018035",
    val supportEmail: String = "sentrystore.pk@gmail.com",
    val website: String = "https://sentrystore.pk",
    val logoBase64: String = "",
    val isSetupCompleted: Boolean = true
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val action: String = "",
    val module: String = "",
    val details: String = "",
    val performedBy: String = "System",
    val timestamp: Long = System.currentTimeMillis()
)
