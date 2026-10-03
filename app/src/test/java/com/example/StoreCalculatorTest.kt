package com.example

import com.example.util.StoreCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreCalculatorTest {

    @Test
    fun testBasicArithmetic() {
        assertEquals(380.0, StoreCalculator.evaluateExpression("180 + 200"), 0.001)
        assertEquals(150.0, StoreCalculator.evaluateExpression("500 - 350"), 0.001)
        assertEquals(1900.0, StoreCalculator.evaluateExpression("5 * 380"), 0.001)
        assertEquals(125.0, StoreCalculator.evaluateExpression("500 / 4"), 0.001)
    }

    @Test
    fun testDecimalAndNegatives() {
        assertEquals(475.0, StoreCalculator.evaluateExpression("1.25 * 380"), 0.001)
        assertEquals(-50.0, StoreCalculator.evaluateExpression("100 - 150"), 0.001)
        assertEquals(-200.0, StoreCalculator.evaluateExpression("-50 * 4"), 0.001)
    }

    @Test
    fun testPercentageCalculation() {
        // 10% of 2000 = 200
        assertEquals(200.0, StoreCalculator.evaluateExpression("2000 % 10"), 0.001)
    }

    @Test
    fun testDiscountCalculation() {
        // Price: 1500, Discount 10% -> Discount Amount: 150, Net: 1350
        val res = StoreCalculator.calculateDiscount(1500.0, 10.0)
        assertEquals(150.0, res.discountAmount, 0.001)
        assertEquals(1350.0, res.netPayable, 0.001)
    }

    @Test
    fun testTaxCalculation() {
        // Subtotal: 1000, Tax 17% -> Tax: 170, Gross: 1170
        val res = StoreCalculator.calculateTax(1000.0, 17.0)
        assertEquals(170.0, res.taxAmount, 0.001)
        assertEquals(1170.0, res.grossTotal, 0.001)
    }

    @Test
    fun testProfitAndMargin() {
        // Purchase: 320, Sale: 400 -> Profit: 80, Margin: 20%
        val res = StoreCalculator.calculateProfit(salePrice = 400.0, purchasePrice = 320.0)
        assertEquals(80.0, res.grossProfit, 0.001)
        assertEquals(20.0, res.marginPercent, 0.001)
    }

    @Test
    fun testFormatting() {
        assertEquals("Rs 1,250.00", StoreCalculator.formatCurrency(1250.0))
        assertEquals("Rs 0.00", StoreCalculator.formatCurrency(0.0))
    }
}
