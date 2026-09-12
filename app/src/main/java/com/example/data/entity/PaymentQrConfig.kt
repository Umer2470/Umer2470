package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "payment_qr_configs")
data class PaymentQrConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "Easypaisa",
    val paymentType: String = "Easypaisa", // Easypaisa, JazzCash, Raast, Bank, Custom
    val imagePath: String = "",
    val accountTitle: String = "",
    val accountNumber: String = "",
    val instructions: String = "Scan QR to pay directly",
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
