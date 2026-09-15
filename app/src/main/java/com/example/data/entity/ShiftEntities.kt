package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "register_shifts")
data class RegisterShift(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shiftNumber: String = "",
    val cashierName: String = "",
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val openingCash: Double = 0.0,
    val openingNotes: String = "",
    val status: String = "OPEN", // "OPEN", "CLOSED"
    val cashSales: Double = 0.0,
    val cardSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val totalSales: Double = 0.0,
    val cashIn: Double = 0.0,
    val cashOut: Double = 0.0,
    val expectedCash: Double = 0.0,
    val actualCash: Double = 0.0,
    val discrepancy: Double = 0.0, // actualCash - expectedCash (positive = over, negative = short)
    val closingNotes: String = "",
    val totalInvoices: Int = 0,
    val branchId: Long = 1,
    val denomination5000: Int = 0,
    val denomination1000: Int = 0,
    val denomination500: Int = 0,
    val denomination100: Int = 0,
    val denomination50: Int = 0,
    val denomination20: Int = 0,
    val denomination10: Int = 0,
    val denominationCoins: Double = 0.0,
    val closedBy: String = ""
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "cash_movements")
data class CashMovement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shiftId: Long = 0,
    val type: String = "CASH_IN", // "CASH_IN", "CASH_OUT"
    val amount: Double = 0.0,
    val reason: String = "",
    val cashierName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
