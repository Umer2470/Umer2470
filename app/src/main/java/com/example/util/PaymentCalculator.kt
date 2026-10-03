package com.example.util

import com.example.data.entity.PaymentStatus
import kotlin.math.max
import kotlin.math.min

data class PaymentCalculationResult(
    val invoiceTotal: Double,
    val amountReceived: Double,
    val amountApplied: Double,
    val changeDue: Double,
    val balanceDue: Double,
    val status: PaymentStatus
)

object PaymentCalculator {

    fun calculate(invoiceTotal: Double, amountReceived: Double): PaymentCalculationResult {
        val total = max(0.0, invoiceTotal)
        val received = max(0.0, amountReceived)

        val applied = min(total, received)
        val change = max(0.0, received - total)
        val balance = max(0.0, total - applied)

        val status = when {
            balance <= 0.001 -> PaymentStatus.PAID
            applied > 0.0 -> PaymentStatus.PARTIALLY_PAID
            else -> PaymentStatus.UNPAID
        }

        return PaymentCalculationResult(
            invoiceTotal = total,
            amountReceived = received,
            amountApplied = applied,
            changeDue = change,
            balanceDue = balance,
            status = status
        )
    }

    fun calculateSettlement(currentBalanceDue: Double, paymentAmount: Double): Double {
        val balance = max(0.0, currentBalanceDue)
        val payment = max(0.0, paymentAmount)
        return min(balance, payment)
    }
}
