package com.example

import com.example.util.InvoiceNumberService
import org.junit.Assert.*
import org.junit.Test

class InvoiceNumberServiceTest {

    @Test
    fun testExactFormattingRequirements() {
        // Strict format required: INV. + 5-digit zero-padded serial
        assertEquals("INV.00001", InvoiceNumberService.formatInvoiceNumber(1))
        assertEquals("INV.00002", InvoiceNumberService.formatInvoiceNumber(2))
        assertEquals("INV.00003", InvoiceNumberService.formatInvoiceNumber(3))
        assertEquals("INV.00004", InvoiceNumberService.formatInvoiceNumber(4))
        assertEquals("INV.00005", InvoiceNumberService.formatInvoiceNumber(5))
        assertEquals("INV.00009", InvoiceNumberService.formatInvoiceNumber(9))
        assertEquals("INV.00010", InvoiceNumberService.formatInvoiceNumber(10))
        assertEquals("INV.00099", InvoiceNumberService.formatInvoiceNumber(99))
        assertEquals("INV.00999", InvoiceNumberService.formatInvoiceNumber(999))
        assertEquals("INV.09999", InvoiceNumberService.formatInvoiceNumber(9999))
        assertEquals("INV.10000", InvoiceNumberService.formatInvoiceNumber(10000))
        assertEquals("INV.10001", InvoiceNumberService.formatInvoiceNumber(10001))
    }

    @Test
    fun testCustomPrefixAndFallback() {
        assertEquals("INV.00001", InvoiceNumberService.formatInvoiceNumber(1, ""))
        assertEquals("BILL.00042", InvoiceNumberService.formatInvoiceNumber(42, "BILL."))
    }

    @Test
    fun testExtractSerial() {
        assertEquals(1L, InvoiceNumberService.extractSerial("INV.00001"))
        assertEquals(2L, InvoiceNumberService.extractSerial("INV.00002"))
        assertEquals(25L, InvoiceNumberService.extractSerial("INV.00025"))
        assertEquals(999L, InvoiceNumberService.extractSerial("INV.00999"))
        assertEquals(10000L, InvoiceNumberService.extractSerial("INV.10000"))
        assertEquals(123456L, InvoiceNumberService.extractSerial("INV.123456"))

        // Legacy format should return null (safely ignored so as not to pollute sequence)
        assertNull(InvoiceNumberService.extractSerial("INV-849102"))
        assertNull(InvoiceNumberService.extractSerial("RET-123456"))
        assertNull(InvoiceNumberService.extractSerial(""))
        assertNull(InvoiceNumberService.extractSerial("INVALID"))
    }

    @Test
    fun testStrictSequentialProgression() {
        var currentSerial = 0L
        val assignedNumbers = mutableListOf<String>()

        for (i in 1..25) {
            currentSerial += 1L
            val formatted = InvoiceNumberService.formatInvoiceNumber(currentSerial)
            assignedNumbers.add(formatted)
        }

        assertEquals(25, assignedNumbers.size)
        assertEquals("INV.00001", assignedNumbers.first())
        assertEquals("INV.00025", assignedNumbers.last())
        // Distinctness check
        assertEquals(25, assignedNumbers.distinct().size)

        // Simulate return/void of INV.00010 and INV.00025:
        // Next invoice must continue from 25 + 1 -> INV.00026, never reusing INV.00010
        val nextSerial = currentSerial + 1L
        val nextFormatted = InvoiceNumberService.formatInvoiceNumber(nextSerial)
        assertEquals("INV.00026", nextFormatted)
        assertFalse("Never reuse existing numbers", assignedNumbers.contains(nextFormatted))
    }
}
