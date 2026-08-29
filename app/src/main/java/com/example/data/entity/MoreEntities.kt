package com.example.data.entity

import androidx.room.Entity
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
    val paperWidthMm: Int = 80 // 58mm or 80mm
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
    val dateString: String = "", // yyyy-MM-dd
    val checkInTime: Long = 0,
    val checkOutTime: Long = 0,
    val status: String = "Present", // Present, Late, Absent, Half-Day
    val branchId: Long = 1,
    val notes: String = ""
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
