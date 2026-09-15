package com.example.data.fbr

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Official FBR Digital Invoicing / Tier-1 POS integration request payload models.
 */
@JsonClass(generateAdapter = true)
data class FbrInvoicePayload(
    @Json(name = "InvoiceNumber") val invoiceNumber: String = "",
    @Json(name = "POSID") val posId: Int = 0,
    @Json(name = "USIN") val usin: String = "",
    @Json(name = "DateTime") val dateTime: String = "",
    @Json(name = "BuyerNTN") val buyerNtn: String = "",
    @Json(name = "BuyerCNIC") val buyerCnic: String = "",
    @Json(name = "BuyerName") val buyerName: String = "",
    @Json(name = "BuyerPhoneNumber") val buyerPhoneNumber: String = "",
    @Json(name = "TotalSaleValue") val totalSaleValue: Double = 0.0,
    @Json(name = "TotalTaxCharged") val totalTaxCharged: Double = 0.0,
    @Json(name = "TotalQuantity") val totalQuantity: Double = 0.0,
    @Json(name = "TotalBillAmount") val totalBillAmount: Double = 0.0,
    @Json(name = "PaymentMode") val paymentMode: Int = 1, // 1: Cash, 2: Card, 3: Cheque, 4: Credit, 5: Other
    @Json(name = "InvoiceType") val invoiceType: Int = 1, // 1: New, 2: Debit/Credit, 3: Return
    @Json(name = "Items") val items: List<FbrInvoiceItemPayload> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FbrInvoiceItemPayload(
    @Json(name = "ItemCode") val itemCode: String = "",
    @Json(name = "ItemName") val itemName: String = "",
    @Json(name = "Quantity") val quantity: Double = 1.0,
    @Json(name = "PCTCode") val pctCode: String = "00000000",
    @Json(name = "TaxRate") val taxRate: Double = 0.0,
    @Json(name = "SaleValue") val saleValue: Double = 0.0,
    @Json(name = "TotalAmount") val totalAmount: Double = 0.0,
    @Json(name = "TaxCharged") val taxCharged: Double = 0.0,
    @Json(name = "Discount") val discount: Double = 0.0,
    @Json(name = "FurtherTax") val furtherTax: Double = 0.0,
    @Json(name = "InvoiceType") val invoiceType: Int = 1
)

@JsonClass(generateAdapter = true)
data class FbrInvoiceResponse(
    @Json(name = "InvoiceNumber") val invoiceNumber: String? = null,
    @Json(name = "Response") val response: String? = null,
    @Json(name = "Code") val code: String? = null,
    @Json(name = "Status") val status: String? = null,
    @Json(name = "Errors") val errors: List<String>? = null
)

sealed class FbrSubmissionResult {
    data class Success(
        val fbrInvoiceNumber: String,
        val responseCode: String,
        val qrCodeData: String,
        val message: String
    ) : FbrSubmissionResult()

    data class Failure(
        val errorMessage: String,
        val responseCode: String = "",
        val isNetworkError: Boolean = false,
        val retryable: Boolean = true
    ) : FbrSubmissionResult()

    object NotRequired : FbrSubmissionResult()
}

enum class FbrConnectionStatus {
    OFFLINE,
    CONFIGURATION_REQUIRED,
    CONNECTING,
    CONNECTED,
    AUTHENTICATION_FAILED,
    API_ERROR
}
