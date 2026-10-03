package com.example.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

data class DiscountResult(
    val originalPrice: Double,
    val discountPercent: Double,
    val discountAmount: Double,
    val netPayable: Double
)

data class TaxResult(
    val subtotal: Double,
    val taxPercent: Double,
    val taxAmount: Double,
    val grossTotal: Double
)

data class ProfitResult(
    val purchasePrice: Double,
    val salePrice: Double,
    val grossProfit: Double,
    val marginPercent: Double
)

object StoreCalculator {

    private val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun formatCurrency(amount: Double): String {
        return "Rs ${numberFormat.format(amount)}"
    }

    fun formatNumber(amount: Double): String {
        return numberFormat.format(amount)
    }

    /**
     * Quantity × Unit Price
     */
    fun calculateQuantityPrice(quantity: Double, unitPrice: Double): Double {
        return max(0.0, quantity * unitPrice)
    }

    /**
     * Discount Calculation: Net = Price - (Price * % / 100)
     */
    fun calculateDiscount(price: Double, discountPercent: Double): DiscountResult {
        val safePrice = max(0.0, price)
        val safeDiscount = max(0.0, discountPercent)
        val discountAmount = safePrice * (safeDiscount / 100.0)
        val netPayable = max(0.0, safePrice - discountAmount)
        return DiscountResult(
            originalPrice = safePrice,
            discountPercent = safeDiscount,
            discountAmount = discountAmount,
            netPayable = netPayable
        )
    }

    /**
     * Tax Calculation: Gross = Subtotal + (Subtotal * % / 100)
     */
    fun calculateTax(subtotal: Double, taxPercent: Double): TaxResult {
        val safeSubtotal = max(0.0, subtotal)
        val safeTax = max(0.0, taxPercent)
        val taxAmount = safeSubtotal * (safeTax / 100.0)
        val grossTotal = safeSubtotal + taxAmount
        return TaxResult(
            subtotal = safeSubtotal,
            taxPercent = safeTax,
            taxAmount = taxAmount,
            grossTotal = grossTotal
        )
    }

    /**
     * Profit & Margin: Profit = Sale - Purchase; Margin = (Profit / Sale) * 100
     */
    fun calculateProfit(salePrice: Double, purchasePrice: Double): ProfitResult {
        val safeSale = max(0.0, salePrice)
        val safePurchase = max(0.0, purchasePrice)
        val grossProfit = safeSale - safePurchase
        val marginPercent = if (safeSale > 0.0) (grossProfit / safeSale) * 100.0 else 0.0
        return ProfitResult(
            purchasePrice = safePurchase,
            salePrice = safeSale,
            grossProfit = grossProfit,
            marginPercent = marginPercent
        )
    }

    /**
     * Safely evaluates arithmetic expressions with +, -, *, /, % and decimals.
     */
    fun evaluateExpression(expr: String): Double {
        val clean = expr.replace(" ", "").replace("×", "*").replace("÷", "/")
        if (clean.isBlank()) return 0.0

        return try {
            ExpressionParser(clean).parse()
        } catch (e: Exception) {
            0.0
        }
    }

    private class ExpressionParser(private val str: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) return x
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val divisor = parseFactor()
                        x = if (divisor != 0.0) x / divisor else 0.0
                    }
                    eat('%'.code) -> x = (x * parseFactor()) / 100.0
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                val sub = str.substring(startPos, pos)
                x = sub.toDoubleOrNull() ?: 0.0
            } else {
                x = 0.0
            }
            return x
        }
    }
}
