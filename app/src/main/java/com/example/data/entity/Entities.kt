package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val category: String = "General",
    val barcode: String = "",
    val purchasePrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val stockQuantity: Double = 0.0,
    val unit: String = "Pcs",
    val minStockAlert: Double = 5.0,
    val description: String = "",
    val branchId: Long = 1,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String = "",
    val customerId: Long = 0,
    val customerName: String = "Walk-in Customer",
    val totalAmount: Double = 0.0,
    val discount: Double = 0.0,
    val taxRate: Double = 0.0,
    val taxAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val paymentType: String = "Cash", // Cash, Bank, Credit
    val notes: String = "",
    val cashierName: String = "",
    val branchId: Long = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long = 0,
    val productId: Long = 0,
    val productName: String = "",
    val quantity: Double = 1.0,
    val unit: String = "Pcs",
    val purchasePrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val totalPrice: Double = 0.0
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val balance: Double = 0.0, // positive means customer owes money
    val notes: String = "",
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val companyName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val balance: Double = 0.0, // positive means we owe supplier
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billNumber: String = "",
    val supplierId: Long = 0,
    val supplierName: String = "",
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val paymentType: String = "Cash",
    val notes: String = "",
    val branchId: Long = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "purchase_items")
data class PurchaseItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purchaseId: Long = 0,
    val productId: Long = 0,
    val productName: String = "",
    val quantity: Double = 1.0,
    val unit: String = "Pcs",
    val unitCost: Double = 0.0,
    val totalCost: Double = 0.0
)
