package com.example.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

class EscPosThermalPrinterService(private val context: Context) {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    suspend fun printReceipt(
        deviceAddress: String,
        invoice: PrintableInvoice,
        paperWidthMm: Int = 80
    ): Boolean = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@withContext false
        if (!adapter.isEnabled) return@withContext false

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket.connect()
            outputStream = socket.outputStream

            val columns = if (paperWidthMm <= 58) 32 else 48
            val rawText = InvoiceFormattingService.generateThermalText(invoice, columns)
            val bytes = generateEscPosCommandBytes(invoice, paperWidthMm)

            outputStream.write(bytes)
            outputStream.flush()
            true
        } catch (e: Exception) {
            false
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    companion object {
        fun generateEscPosCommandBytes(
            invoice: PrintableInvoice,
            paperWidthMm: Int = 80
        ): ByteArray {
            val columns = if (paperWidthMm <= 58) 32 else 48
            val rawText = InvoiceFormattingService.generateThermalText(invoice, columns)
            val textBytes = rawText.toByteArray(Charsets.UTF_8)
            val initCommand = byteArrayOf(0x1B, 0x40)
            val cutCommand = byteArrayOf(0x0A, 0x0A, 0x0A, 0x1D, 0x56, 0x42, 0x00)
            return initCommand + textBytes + cutCommand
        }

        fun generateEscPosCommandBytes(
            sale: com.example.data.entity.Sale,
            items: List<com.example.data.entity.SaleItem>,
            settings: com.example.data.entity.StoreSettings,
            paperWidthMm: Int = 80
        ): ByteArray {
            val invoice = InvoiceFormattingService.formatSaleTransaction(sale, items, settings)
            return generateEscPosCommandBytes(invoice, paperWidthMm)
        }
    }
}
