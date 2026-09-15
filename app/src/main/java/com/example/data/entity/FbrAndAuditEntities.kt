package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "sale_returns")
data class SaleReturn(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val returnNumber: String = "",
    val saleId: Long = 0,
    val originalInvoiceNumber: String = "",
    val customerId: Long = 0,
    val customerName: String = "Walk-in Customer",
    val refundAmount: Double = 0.0,
    val refundPaymentType: String = "Cash", // Cash, Credit, Bank
    val taxRefundAmount: Double = 0.0,
    val reason: String = "",
    val processedBy: String = "",
    val branchId: Long = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "sale_return_items")
data class SaleReturnItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val returnId: Long = 0,
    val saleItemId: Long = 0,
    val productId: Long = 0,
    val productName: String = "",
    val quantityReturned: Double = 1.0,
    val unit: String = "Pcs",
    val unitPrice: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val totalRefund: Double = 0.0
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "stock_movements")
data class StockMovement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long = 0,
    val productName: String = "",
    val quantityDelta: Double = 0.0,
    val stockBefore: Double = 0.0,
    val stockAfter: Double = 0.0,
    val movementType: String = "SALE", // SALE, PURCHASE, RETURN, DAMAGE, MANUAL_ADJUSTMENT, INITIAL_STOCK
    val referenceId: String = "", // Invoice #, Bill #, Return #, etc.
    val reason: String = "",
    val performedBy: String = "System",
    val branchId: Long = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "fbr_invoice_records")
data class FbrInvoiceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long = 0,
    val invoiceNumber: String = "",
    val usin: String = "",
    val posId: String = "",
    val ntn: String = "",
    val strn: String = "",
    val totalTaxableAmount: Double = 0.0,
    val totalTaxAmount: Double = 0.0,
    val invoiceGrandTotal: Double = 0.0,
    val submissionStatus: String = "PENDING", // PENDING, SUBMITTING, SUBMITTED, ACKNOWLEDGED, FAILED, RETRY_REQUIRED, NOT_REQUIRED
    val fbrInvoiceNumber: String = "",
    val qrCodeData: String = "",
    val responseCode: String = "",
    val responseMessage: String = "",
    val submittedAt: Long? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
