package com.example.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

data class PrinterConfig(
    val macAddress: String?,
    val deviceName: String?,
    val paperWidthMm: Int = 80
)

enum class QuickPrintStatusType {
    BLUETOOTH_PRINTED,
    SYSTEM_PRINT_SPOOLED,
    FAILED
}

data class QuickPrintResult(
    val type: QuickPrintStatusType,
    val message: String
)

class EscPosThermalPrinterService(private val context: Context) {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
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
        private const val PREFS_NAME = "pos_thermal_printer_prefs"
        private const val KEY_LAST_PRINTER_MAC = "last_connected_printer_mac"
        private const val KEY_LAST_PRINTER_NAME = "last_connected_printer_name"
        private const val KEY_LAST_PAPER_WIDTH = "last_paper_width_mm"

        fun getPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        fun getLastPrinterConfig(context: Context, defaultWidth: Int = 80): PrinterConfig {
            val prefs = getPrefs(context)
            val mac = prefs.getString(KEY_LAST_PRINTER_MAC, null)
            val name = prefs.getString(KEY_LAST_PRINTER_NAME, null)
            val width = prefs.getInt(KEY_LAST_PAPER_WIDTH, defaultWidth)
            return PrinterConfig(macAddress = mac, deviceName = name, paperWidthMm = width)
        }

        fun savePrinterConfig(context: Context, macAddress: String?, name: String?, paperWidthMm: Int = 80) {
            val prefs = getPrefs(context)
            prefs.edit().apply {
                if (macAddress != null) putString(KEY_LAST_PRINTER_MAC, macAddress) else remove(KEY_LAST_PRINTER_MAC)
                if (name != null) putString(KEY_LAST_PRINTER_NAME, name) else remove(KEY_LAST_PRINTER_NAME)
                putInt(KEY_LAST_PAPER_WIDTH, paperWidthMm)
                apply()
            }
        }

        @SuppressLint("MissingPermission")
        fun getPairedBluetoothPrinters(context: Context): List<Pair<String, String>> {
            return try {
                val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
                if (!adapter.isEnabled) return emptyList()
                adapter.bondedDevices?.map { device ->
                    Pair(device.address, device.name ?: "Thermal Printer")
                } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        /**
         * One-Tap Quick Re-Print receipt using last connected thermal printer configuration.
         * Attempts ESC/POS Bluetooth direct stream first if device is configured;
         * falls back cleanly to Android PrintManager thermal spooler.
         */
        suspend fun quickRePrintReceipt(
            context: Context,
            sale: Sale,
            items: List<SaleItem>,
            settings: StoreSettings?
        ): QuickPrintResult = withContext(Dispatchers.IO) {
            val effectiveSettings = settings ?: StoreSettings()
            val paperWidth = effectiveSettings.paperWidthMm
            val printableInvoice = InvoiceFormattingService.formatSaleTransaction(sale, items, effectiveSettings)
            val savedConfig = getLastPrinterConfig(context, paperWidth)

            // 1. If Bluetooth MAC is configured, try direct ESC/POS hardware print
            if (!savedConfig.macAddress.isNullOrBlank()) {
                val service = EscPosThermalPrinterService(context)
                val success = service.printReceipt(savedConfig.macAddress, printableInvoice, paperWidth)
                if (success) {
                    return@withContext QuickPrintResult(
                        type = QuickPrintStatusType.BLUETOOTH_PRINTED,
                        message = "Receipt printed via Bluetooth Thermal Printer (${savedConfig.deviceName ?: savedConfig.macAddress} • ${paperWidth}mm)"
                    )
                }
            }

            // 2. Fallback / Standard: Fast Android System PrintManager with formatted thermal receipt PDF
            withContext(Dispatchers.Main) {
                val thermalPdf = PdfGenerator.generateInvoicePdf(
                    context,
                    printableInvoice,
                    PdfGenerator.ReceiptFormat.THERMAL_80MM
                )
                if (thermalPdf != null) {
                    PdfGenerator.printPdfFile(
                        context,
                        thermalPdf,
                        "ThermalPrint_${sale.invoiceNumber}"
                    )
                }
            }

            return@withContext QuickPrintResult(
                type = QuickPrintStatusType.SYSTEM_PRINT_SPOOLED,
                message = "Receipt sent to Thermal Print Spooler (${paperWidth}mm • #${sale.invoiceNumber})"
            )
        }

        fun generateEscPosCommandBytes(
            invoice: PrintableInvoice,
            paperWidthMm: Int = 80
        ): ByteArray {
            val columns = if (paperWidthMm <= 58) 32 else 48
            val rawText = InvoiceFormattingService.generateThermalText(invoice, columns)
            val textBytes = rawText.toByteArray(Charsets.UTF_8)
            val initCommand = byteArrayOf(0x1B, 0x40) // ESC @ (Initialize printer)
            val cutCommand = byteArrayOf(0x0A, 0x0A, 0x0A, 0x1D, 0x56, 0x42, 0x00) // GS V B 0 (Cut paper)
            return initCommand + textBytes + cutCommand
        }

        fun generateEscPosCommandBytes(
            sale: Sale,
            items: List<SaleItem>,
            settings: StoreSettings,
            paperWidthMm: Int = 80
        ): ByteArray {
            val invoice = InvoiceFormattingService.formatSaleTransaction(sale, items, settings)
            return generateEscPosCommandBytes(invoice, paperWidthMm)
        }
    }
}

