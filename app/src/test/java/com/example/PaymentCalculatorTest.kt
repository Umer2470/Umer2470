package com.example

import com.example.data.entity.PaymentStatus
import com.example.util.PaymentCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentCalculatorTest {

    @Test
    fun testScenarioA_OverpaymentReturnsChange() {
        // Invoice Rs. 5,000; customer gives Rs. 6,000
        val result = PaymentCalculator.calculate(invoiceTotal = 5000.0, amountReceived = 6000.0)

        assertEquals(5000.0, result.invoiceTotal, 0.001)
        assertEquals(6000.0, result.amountReceived, 0.001)
        assertEquals(5000.0, result.amountApplied, 0.001)
        assertEquals(1000.0, result.changeDue, 0.001)
        assertEquals(0.0, result.balanceDue, 0.001)
        assertEquals(PaymentStatus.PAID, result.status)
    }

    @Test
    fun testScenarioB_UnderpaymentRecordsBalanceDue() {
        // Invoice Rs. 5,000; customer gives Rs. 4,800
        val result = PaymentCalculator.calculate(invoiceTotal = 5000.0, amountReceived = 4800.0)

        assertEquals(5000.0, result.invoiceTotal, 0.001)
        assertEquals(4800.0, result.amountReceived, 0.001)
        assertEquals(4800.0, result.amountApplied, 0.001)
        assertEquals(0.0, result.changeDue, 0.001)
        assertEquals(200.0, result.balanceDue, 0.001)
        assertEquals(PaymentStatus.PARTIALLY_PAID, result.status)
    }

    @Test
    fun testScenarioC_ExactPayment() {
        // Invoice Rs. 5,000; customer gives Rs. 5,000
        val result = PaymentCalculator.calculate(invoiceTotal = 5000.0, amountReceived = 5000.0)

        assertEquals(5000.0, result.invoiceTotal, 0.001)
        assertEquals(5000.0, result.amountReceived, 0.001)
        assertEquals(5000.0, result.amountApplied, 0.001)
        assertEquals(0.0, result.changeDue, 0.001)
        assertEquals(0.0, result.balanceDue, 0.001)
        assertEquals(PaymentStatus.PAID, result.status)
    }

    @Test
    fun testSettlementCalculation_CannotExceedOutstandingBalance() {
        val balanceDue = 200.0
        // Customer attempts to pay 300 against a 200 debt
        val applied = PaymentCalculator.calculateSettlement(balanceDue, 300.0)
        assertEquals(200.0, applied, 0.001)

        // Customer pays partial settlement of 150
        val partialApplied = PaymentCalculator.calculateSettlement(balanceDue, 150.0)
        assertEquals(150.0, partialApplied, 0.001)
    }
}
