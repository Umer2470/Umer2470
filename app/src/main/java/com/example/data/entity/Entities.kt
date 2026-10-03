package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String,
    val category: String = "General",
    val purchasePrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val stockQuantity: Double = 0.0,
    val minStockAlert: Double = 5.0,
    val unit: String = "Pcs",
    val storeId: Long = 1,
    val updatedAt: Long = 0L
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val currentBalance: Double = 0.0,
    val storeId: Long = 1,
    val updatedAt: Long = 0L
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contactPerson: String = "",
    val phone: String = "",
    val balance: Double = 0.0,
    val storeId: Long = 1
)

enum class PaymentStatus {
    PAID,
    PARTIALLY_PAID,
    UNPAID
}

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNo: String,
    val customerId: Long? = null,
    val customerName: String = "Walking Customer",
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val amountReceived: Double = 0.0,
    val amountApplied: Double = 0.0,
    val changeDue: Double = 0.0,
    val balanceDue: Double = 0.0,
    val paymentMethod: String = "CASH",
    val paymentStatus: String = "PAID",
    val cashierName: String = "Cashier",
    val storeId: Long = 1,
    val syncStatus: String = "PENDING",
    val createdAt: Long = 0L
)

@Entity(tableName = "invoice_items")
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long = 0,
    val productId: Long = 0,
    val productName: String = "",
    val barcode: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val storeId: Long = 1
)

@Entity(tableName = "payment_records")
data class PaymentRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long = 0,
    val invoiceNo: String = "",
    val customerId: Long? = null,
    val amount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val cashierName: String = "Cashier",
    val notes: String = "",
    val storeId: Long = 1,
    val idempotencyKey: String = "",
    val createdAt: Long = 0L
)

@Entity(tableName = "customer_ledger")
data class CustomerLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long = 0,
    val invoiceId: Long? = null,
    val transactionType: String = "",
    val debitAmount: Double = 0.0,
    val creditAmount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val description: String = "",
    val storeId: Long = 1,
    val createdAt: Long = 0L
)
