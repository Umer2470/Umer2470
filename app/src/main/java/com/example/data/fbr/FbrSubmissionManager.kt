package com.example.data.fbr

import android.util.Log
import com.example.data.dao.ActivityLogDao
import com.example.data.dao.FbrInvoiceRecordDao
import com.example.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class FbrSubmissionManager(
    private val fbrApiService: FbrApiService,
    private val fbrRecordDao: FbrInvoiceRecordDao,
    private val activityLogDao: ActivityLogDao
) {

    companion object {
        private const val TAG = "FbrSubmissionManager"
    }

    private val fbrDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Submit a completed sale to FBR if FBR integration is enabled.
     */
    suspend fun processSaleSubmission(
        sale: Sale,
        saleItems: List<SaleItem>,
        settings: StoreSettings,
        customer: Customer? = null
    ): FbrSubmissionResult = withContext(Dispatchers.IO) {
        if (!settings.isFbrIntegrationEnabled) {
            return@withContext FbrSubmissionResult.NotRequired
        }

        // Check if already submitted or acknowledged
        val existing = fbrRecordDao.getRecordForSale(sale.id)
        if (existing != null && (existing.submissionStatus == "ACKNOWLEDGED" || existing.submissionStatus == "SUBMITTED")) {
            Log.d(TAG, "Invoice ${sale.invoiceNumber} already submitted with status ${existing.submissionStatus}")
            return@withContext FbrSubmissionResult.Success(
                fbrInvoiceNumber = existing.fbrInvoiceNumber,
                responseCode = existing.responseCode,
                qrCodeData = existing.qrCodeData,
                message = "Already submitted"
            )
        }

        val posIdInt = settings.fbrPosId.toIntOrNull() ?: 0
        val usin = "USIN-${sale.id}-${sale.invoiceNumber}"

        // Create or update local FBR record in PENDING status first (Offline Safety)
        val initialRecord = existing ?: FbrInvoiceRecord(
            saleId = sale.id,
            invoiceNumber = sale.invoiceNumber,
            usin = usin,
            posId = settings.fbrPosId,
            ntn = settings.fbrNtn,
            strn = settings.fbrStrn,
            totalTaxableAmount = (sale.totalAmount - sale.discount).coerceAtLeast(0.0),
            totalTaxAmount = sale.taxAmount,
            invoiceGrandTotal = sale.netAmount,
            submissionStatus = "PENDING"
        )
        val recordId = fbrRecordDao.insertRecord(initialRecord)

        // Build Payload
        val paymentModeInt = when (sale.paymentType.lowercase()) {
            "cash" -> 1
            "card", "bank" -> 2
            "credit" -> 4
            else -> 1
        }

        val payloadItems = saleItems.map { item ->
            FbrInvoiceItemPayload(
                itemCode = item.productId.toString(),
                itemName = item.productName,
                quantity = item.quantity,
                pctCode = "00000000",
                taxRate = sale.taxRate,
                saleValue = item.totalPrice,
                totalAmount = item.totalPrice,
                taxCharged = if (sale.netAmount > 0) (item.totalPrice / sale.netAmount) * sale.taxAmount else 0.0,
                discount = 0.0,
                invoiceType = 1
            )
        }

        val payload = FbrInvoicePayload(
            invoiceNumber = "",
            posId = posIdInt,
            usin = usin,
            dateTime = fbrDateFormat.format(Date(sale.createdAt)),
            buyerNtn = "",
            buyerCnic = "",
            buyerName = customer?.name ?: sale.customerName,
            buyerPhoneNumber = customer?.phone ?: "",
            totalSaleValue = (sale.totalAmount - sale.discount).coerceAtLeast(0.0),
            totalTaxCharged = sale.taxAmount,
            totalQuantity = saleItems.sumOf { it.quantity },
            totalBillAmount = sale.netAmount,
            paymentMode = paymentModeInt,
            invoiceType = 1,
            items = payloadItems
        )

        // Submit to FBR API
        val isProduction = settings.fbrEnvironment.equals("Live", ignoreCase = true) ||
                settings.fbrEnvironment.equals("Production", ignoreCase = true)

        val result = fbrApiService.submitInvoice(
            payload = payload,
            authToken = settings.fbrApiAuthToken,
            isProduction = isProduction
        )

        when (result) {
            is FbrSubmissionResult.Success -> {
                fbrRecordDao.updateRecord(
                    initialRecord.copy(
                        id = recordId,
                        submissionStatus = "ACKNOWLEDGED",
                        fbrInvoiceNumber = result.fbrInvoiceNumber,
                        qrCodeData = result.qrCodeData,
                        responseCode = result.responseCode,
                        responseMessage = result.message,
                        submittedAt = System.currentTimeMillis()
                    )
                )
                activityLogDao.insertLog(
                    ActivityLog(
                        action = "FBR_INVOICE_SUBMITTED",
                        module = "FBR_TAX",
                        details = "Invoice #${sale.invoiceNumber} submitted to FBR. Ref: ${result.fbrInvoiceNumber}",
                        performedBy = sale.cashierName.ifBlank { "Cashier" }
                    )
                )
            }
            is FbrSubmissionResult.Failure -> {
                val newStatus = if (result.isNetworkError) "RETRY_REQUIRED" else "FAILED"
                fbrRecordDao.updateRecord(
                    initialRecord.copy(
                        id = recordId,
                        submissionStatus = newStatus,
                        responseCode = result.responseCode,
                        responseMessage = result.errorMessage,
                        retryCount = initialRecord.retryCount + 1
                    )
                )
                activityLogDao.insertLog(
                    ActivityLog(
                        action = "FBR_SUBMISSION_FAILED",
                        module = "FBR_TAX",
                        details = "FBR submission for Invoice #${sale.invoiceNumber} failed: ${result.errorMessage}",
                        performedBy = sale.cashierName.ifBlank { "Cashier" }
                    )
                )
            }
            is FbrSubmissionResult.NotRequired -> {
                // Do nothing
            }
        }

        return@withContext result
    }

    /**
     * Retry submitting a pending or failed invoice record.
     */
    suspend fun retrySubmission(
        record: FbrInvoiceRecord,
        sale: Sale,
        saleItems: List<SaleItem>,
        settings: StoreSettings
    ): FbrSubmissionResult = withContext(Dispatchers.IO) {
        processSaleSubmission(sale, saleItems, settings)
    }
}
