package com.example.util

import java.util.Locale

object InvoiceNumberService {
    const val DEFAULT_PREFIX = "INV."
    const val DEFAULT_PADDING = 5

    /**
     * Formats a sequential number into standard invoice number format:
     * e.g., 1 -> "INV.00001", 25 -> "INV.00025", 10000 -> "INV.10000"
     */
    fun formatInvoiceNumber(serial: Long, prefix: String = DEFAULT_PREFIX): String {
        val effectivePrefix = if (prefix.isBlank()) DEFAULT_PREFIX else prefix
        return "%s%0${DEFAULT_PADDING}d".format(Locale.US, effectivePrefix, serial)
    }

    /**
     * Extracts the serial number if the invoice number matches standard formats:
     * e.g. "INV.00001" -> 1L, "INV.00025" -> 25L, "INV.10000" -> 10000L.
     * Legacy random timestamps (e.g. INV-849102) are safely ignored.
     */
    fun extractSerial(invoiceNumber: String): Long? {
        val trimmed = invoiceNumber.trim()
        val match = Regex("""^INV\.(\d+)$""", RegexOption.IGNORE_CASE).find(trimmed)
        return match?.groupValues?.get(1)?.toLongOrNull()
    }
}
