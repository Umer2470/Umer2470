package com.example.data.fbr

import com.example.data.entity.Product
import com.example.data.entity.StoreSettings
import kotlin.math.roundToInt

data class TaxCalculationResult(
    val grossSubtotal: Double,
    val discount: Double,
    val taxableAmount: Double,
    val exemptAmount: Double,
    val taxRate: Double,
    val taxAmount: Double,
    val grandTotal: Double,
    val isTaxInclusive: Boolean
)

object FbrTaxService {

    /**
     * Compute tax breakdown for cart items and applied discount based on settings.
     */
    fun calculateTax(
        subtotal: Double,
        discount: Double,
        settings: StoreSettings?,
        items: List<Pair<Product, Double>> = emptyList() // Product to Quantity
    ): TaxCalculationResult {
        if (settings == null || !settings.isFbrIntegrationEnabled) {
            val net = (subtotal - discount).coerceAtLeast(0.0)
            return TaxCalculationResult(
                grossSubtotal = subtotal,
                discount = discount,
                taxableAmount = net,
                exemptAmount = 0.0,
                taxRate = 0.0,
                taxAmount = 0.0,
                grandTotal = net,
                isTaxInclusive = false
            )
        }

        val isInclusive = settings.fbrTaxMode.equals("Inclusive", ignoreCase = true)
        val defaultRate = settings.fbrDefaultTaxRate.coerceAtLeast(0.0)

        // If items breakdown is provided, calculate per-item
        if (items.isNotEmpty()) {
            var taxableSum = 0.0
            var exemptSum = 0.0
            var computedTax = 0.0

            val totalItemGross = items.sumOf { (product, qty) -> product.salePrice * qty }
            val discountRatio = if (totalItemGross > 0) (discount / totalItemGross).coerceIn(0.0, 1.0) else 0.0

            for ((product, qty) in items) {
                val itemGross = product.salePrice * qty
                val itemDiscount = itemGross * discountRatio
                val itemNet = (itemGross - itemDiscount).coerceAtLeast(0.0)

                if (product.isTaxExempt) {
                    exemptSum += itemNet
                } else {
                    val rate = if (product.customTaxRate > 0.0) product.customTaxRate else defaultRate
                    taxableSum += itemNet

                    if (isInclusive) {
                        // Tax included in price: Tax = Net - (Net / (1 + Rate / 100))
                        val baseValue = itemNet / (1.0 + (rate / 100.0))
                        computedTax += (itemNet - baseValue)
                    } else {
                        // Tax exclusive: Tax = Net * (Rate / 100)
                        computedTax += itemNet * (rate / 100.0)
                    }
                }
            }

            val grandTotal = if (isInclusive) {
                taxableSum + exemptSum
            } else {
                taxableSum + exemptSum + computedTax
            }

            return TaxCalculationResult(
                grossSubtotal = subtotal,
                discount = discount,
                taxableAmount = taxableSum,
                exemptAmount = exemptSum,
                taxRate = defaultRate,
                taxAmount = computedTax,
                grandTotal = grandTotal,
                isTaxInclusive = isInclusive
            )
        } else {
            // Flat calculation on subtotal minus discount
            val net = (subtotal - discount).coerceAtLeast(0.0)
            val rate = defaultRate

            val tax = if (isInclusive) {
                val base = net / (1.0 + (rate / 100.0))
                net - base
            } else {
                net * (rate / 100.0)
            }

            val grand = if (isInclusive) net else net + tax

            return TaxCalculationResult(
                grossSubtotal = subtotal,
                discount = discount,
                taxableAmount = if (isInclusive) net - tax else net,
                exemptAmount = 0.0,
                taxRate = rate,
                taxAmount = tax,
                grandTotal = grand,
                isTaxInclusive = isInclusive
            )
        }
    }
}
