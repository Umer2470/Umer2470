package com.example.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.widget.Toast
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object EscPosThermalPrinterService {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    enum class PaperFormat(val maxCols: Int) {
        FORMAT_58MM(32),
        FORMAT_80MM(48)
    }

    /**
     * Generates a complete ESC/POS command byte array for 58mm or 80mm thermal printers.
     */
    fun generateEscPosCommandBytes(
        sale: Sale,
        items: List<SaleItem>,
        settings: StoreSettings,
        format: PaperFormat = PaperFormat.FORMAT_58MM
    ): ByteArray {
        val stream = ByteArrayOutputStream()
        val cols = format.maxCols

        // ESC @ Initialize Printer
        stream.write(byteArrayOf(0x1B, 0x40))

        // ESC a 1 Center align
        stream.write(byteArrayOf(0x1B, 0x61, 0x01))

        // GS ! 0x11 Double Height & Width for Store Title
        stream.write(byteArrayOf(0x1D, 0x21, 0x11))
        stream.write("${settings.storeName.uppercase(Locale.getDefault())}\n".toByteArray(Charsets.UTF_8))

        // Reset Text Size
        stream.write(byteArrayOf(0x1D, 0x21, 0x00))
        if (settings.phone.isNotBlank()) {
            stream.write("Ph: ${settings.phone}\n".toByteArray(Charsets.UTF_8))
        }
        if (settings.address.isNotBlank()) {
            stream.write("${settings.address}\n".toByteArray(Charsets.UTF_8))
        }

        val divider = "=".repeat(cols) + "\n"
        val dashLine = "-".repeat(cols) + "\n"

        stream.write(divider.toByteArray(Charsets.UTF_8))

        // ESC a 0 Left align
        stream.write(byteArrayOf(0x1B, 0x61, 0x00))

        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(sale.timestamp))
        val resolvedCashier = when {
            sale.cashierName.isNotBlank() -> sale.cashierName
            settings.defaultCashierName.isNotBlank() -> settings.defaultCashierName
            else -> "Not Assigned"
        }

        stream.write("Invoice #: ${sale.invoiceNumber}\n".toByteArray(Charsets.UTF_8))
        stream.write("Date:     $formattedDate\n".toByteArray(Charsets.UTF_8))
        stream.write("Customer: ${sale.customerName}\n".toByteArray(Charsets.UTF_8))
        stream.write("Cashier:  $resolvedCashier\n".toByteArray(Charsets.UTF_8))
        stream.write("Pay Mode: ${sale.paymentType}\n".toByteArray(Charsets.UTF_8))
        stream.write(dashLine.toByteArray(Charsets.UTF_8))

        // Header Table Columns
        if (format == PaperFormat.FORMAT_58MM) {
            // 32 Columns: Item (16) Qty (4) Price (12)
            stream.write(formatRow("Item Description", "Qty", "Total", 16, 4, 12).toByteArray(Charsets.UTF_8))
        } else {
            // 48 Columns: Item (24) Qty (6) Rate (8) Total (10)
            stream.write(formatRow4("Item Description", "Qty", "Price", "Total", 24, 6, 8, 10).toByteArray(Charsets.UTF_8))
        }
        stream.write(dashLine.toByteArray(Charsets.UTF_8))

        // Items List
        for (item in items) {
            val pName = item.productName
            val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()} ${item.unit}" else "${item.quantity} ${item.unit}"
            val priceStr = String.format(Locale.US, "%.0f", item.salePrice)
            val totalStr = String.format(Locale.US, "%.0f", item.totalPrice)

            if (format == PaperFormat.FORMAT_58MM) {
                // Multi-line name wrapping for 58mm
                val nameWrapped = wrapText(pName, 16)
                for (i in nameWrapped.indices) {
                    if (i == 0) {
                        stream.write(formatRow(nameWrapped[i], qtyStr, totalStr, 16, 4, 12).toByteArray(Charsets.UTF_8))
                    } else {
                        stream.write(formatRow(nameWrapped[i], "", "", 16, 4, 12).toByteArray(Charsets.UTF_8))
                    }
                }
            } else {
                // 80mm
                val nameWrapped = wrapText(pName, 24)
                for (i in nameWrapped.indices) {
                    if (i == 0) {
                        stream.write(formatRow4(nameWrapped[i], qtyStr, priceStr, totalStr, 24, 6, 8, 10).toByteArray(Charsets.UTF_8))
                    } else {
                        stream.write(formatRow4(nameWrapped[i], "", "", "", 24, 6, 8, 10).toByteArray(Charsets.UTF_8))
                    }
                }
            }
        }

        stream.write(dashLine.toByteArray(Charsets.UTF_8))

        // ESC E 1 Bold On
        stream.write(byteArrayOf(0x1B, 0x45, 0x01))

        // Summary Totals
        val symbol = settings.currencySymbol
        val subtotalStr = "$symbol ${String.format(Locale.US, "%.0f", sale.totalAmount)}"
        val discountStr = "$symbol ${String.format(Locale.US, "%.0f", sale.discount)}"
        val netStr = "$symbol ${String.format(Locale.US, "%.0f", sale.netAmount)}"
        val paidStr = "$symbol ${String.format(Locale.US, "%.0f", sale.paidAmount)}"
        val dueStr = "$symbol ${String.format(Locale.US, "%.0f", sale.dueAmount)}"

        stream.write(formatSummaryRow("Subtotal:", subtotalStr, cols).toByteArray(Charsets.UTF_8))
        if (sale.discount > 0) {
            stream.write(formatSummaryRow("Discount:", "- $discountStr", cols).toByteArray(Charsets.UTF_8))
        }
        stream.write(formatSummaryRow("Grand Total:", netStr, cols).toByteArray(Charsets.UTF_8))
        stream.write(formatSummaryRow("Paid Amount:", paidStr, cols).toByteArray(Charsets.UTF_8))
        if (sale.dueAmount > 0) {
            stream.write(formatSummaryRow("Remaining Due:", dueStr, cols).toByteArray(Charsets.UTF_8))
        }

        // ESC E 0 Bold Off
        stream.write(byteArrayOf(0x1B, 0x45, 0x00))

        stream.write(divider.toByteArray(Charsets.UTF_8))

        // ESC a 1 Center align for QR & Footer
        stream.write(byteArrayOf(0x1B, 0x61, 0x01))

        // Draw ESC/POS QR Code for Invoice
        try {
            val qrBytes = generateEscPosQrCodeBytes(sale.invoiceNumber)
            stream.write(qrBytes)
        } catch (e: Exception) {
            // Fallback text if QR native commands unsupported
            stream.write("[QR: ${sale.invoiceNumber}]\n".toByteArray(Charsets.UTF_8))
        }

        stream.write("\nThank you for shopping with us!\n".toByteArray(Charsets.UTF_8))
        stream.write("Software: AI Commercial POS\n".toByteArray(Charsets.UTF_8))

        // Cash Drawer Kick Command (ESC p 0 25 250)
        stream.write(byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()))

        // Feed paper 4 lines & Cut (GS V 66 0)
        stream.write(byteArrayOf(0x1B, 0x64, 0x04))
        stream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))

        return stream.toByteArray()
    }

    /**
     * ESC/POS QR Code Native Bytes Sequence (Model 2)
     */
    private fun generateEscPosQrCodeBytes(payload: String): ByteArray {
        val stream = ByteArrayOutputStream()
        val data = payload.toByteArray(Charsets.UTF_8)
        val pL = (data.size + 3) % 256
        val pH = (data.size + 3) / 256

        // Set QR model
        stream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))
        // Set QR module size (size 6)
        stream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x06))
        // Set Error correction level M
        stream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x30))
        // Store data in symbol storage area
        stream.write(byteArrayOf(0x1D, 0x28, 0x6B, pL.toByte(), pH.toByte(), 0x31, 0x50, 0x30))
        stream.write(data)
        // Print stored QR symbol
        stream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))

        return stream.toByteArray()
    }

    private fun formatRow(col1: String, col2: String, col3: String, w1: Int, w2: Int, w3: Int): String {
        val c1 = col1.padEnd(w1).take(w1)
        val c2 = col2.padStart(w2).take(w2)
        val c3 = col3.padStart(w3).take(w3)
        return "$c1$c2$c3\n"
    }

    private fun formatRow4(col1: String, col2: String, col3: String, col4: String, w1: Int, w2: Int, w3: Int, w4: Int): String {
        val c1 = col1.padEnd(w1).take(w1)
        val c2 = col2.padStart(w2).take(w2)
        val c3 = col3.padStart(w3).take(w3)
        val c4 = col4.padStart(w4).take(w4)
        return "$c1$c2$c3$c4\n"
    }

    private fun formatSummaryRow(label: String, value: String, maxCols: Int): String {
        val valLen = value.length
        val labelMax = maxCols - valLen - 1
        val truncatedLabel = label.padEnd(labelMax).take(labelMax)
        return truncatedLabel + " " + value + "\n"
    }

    private fun wrapText(text: String, width: Int): List<String> {
        if (text.length <= width) return listOf(text)
        val result = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val end = minOf(start + width, text.length)
            result.add(text.substring(start, end))
            start += width
        }
        return result
    }

    /**
     * Retrieves list of paired Bluetooth devices (Name, Address)
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(context: Context): List<Pair<String, String>> {
        val btAdapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!btAdapter.isEnabled) return emptyList()
        val bonded = btAdapter.bondedDevices ?: return emptyList()
        return bonded.map { device ->
            val name = device.name ?: "Unknown Device"
            val address = device.address ?: ""
            Pair(name, address)
        }
    }

    /**
     * Prints directly to paired Bluetooth Thermal Printer
     */
    @SuppressLint("MissingPermission")
    fun printViaBluetooth(
        context: Context,
        payloadBytes: ByteArray,
        deviceAddress: String? = null,
        onComplete: (Boolean, String) -> Unit
    ) {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null || !btAdapter.isEnabled) {
            onComplete(false, "Bluetooth is disabled or not supported on this device.")
            return
        }

        val pairedDevices: Set<BluetoothDevice> = btAdapter.bondedDevices ?: emptySet()
        val targetDevice = if (!deviceAddress.isNullOrBlank()) {
            pairedDevices.find { it.address == deviceAddress }
        } else {
            // Find first paired device that looks like a printer
            pairedDevices.find {
                it.name?.contains("Printer", ignoreCase = true) == true ||
                it.name?.contains("POS", ignoreCase = true) == true ||
                it.name?.contains("Thermal", ignoreCase = true) == true ||
                it.name?.contains("RP", ignoreCase = true) == true
            } ?: pairedDevices.firstOrNull()
        }

        if (targetDevice == null) {
            onComplete(false, "No paired Bluetooth thermal printer found. Please pair your thermal printer in Android Bluetooth Settings.")
            return
        }

        Thread {
            var socket: BluetoothSocket? = null
            var outputStream: OutputStream? = null
            try {
                socket = targetDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                outputStream = socket.outputStream
                outputStream.write(payloadBytes)
                outputStream.flush()
                Thread.sleep(500)
                onComplete(true, "Sent to Bluetooth printer (${targetDevice.name})")
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, "Failed to communicate with Bluetooth printer: ${e.localizedMessage}")
            } finally {
                try {
                    outputStream?.close()
                    socket?.close()
                } catch (ignored: Exception) {}
            }
        }.start()
    }

    /**
     * Prints directly to USB Connected Thermal Printer
     */
    fun printViaUsb(
        context: Context,
        payloadBytes: ByteArray,
        onComplete: (Boolean, String) -> Unit
    ) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        if (usbManager == null) {
            onComplete(false, "USB Service unavailable.")
            return
        }

        val deviceList = usbManager.deviceList
        if (deviceList.isEmpty()) {
            onComplete(false, "No USB devices connected. Please connect USB thermal printer.")
            return
        }

        var printerDevice: UsbDevice? = null
        for (device in deviceList.values) {
            for (i in 0 until device.interfaceCount) {
                val intf = device.getInterface(i)
                if (intf.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                    printerDevice = device
                    break
                }
            }
            if (printerDevice != null) break
        }

        if (printerDevice == null) {
            // Fallback to first USB device
            printerDevice = deviceList.values.firstOrNull()
        }

        if (printerDevice == null) {
            onComplete(false, "No USB printer detected.")
            return
        }

        try {
            val connection = usbManager.openDevice(printerDevice)
            if (connection == null) {
                onComplete(false, "USB permission required or device busy.")
                return
            }

            var printerInterface = printerDevice.getInterface(0)
            for (i in 0 until printerDevice.interfaceCount) {
                val intf = printerDevice.getInterface(i)
                if (intf.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                    printerInterface = intf
                    break
                }
            }

            connection.claimInterface(printerInterface, true)
            var endpointOut = printerInterface.getEndpoint(0)
            for (i in 0 until printerInterface.endpointCount) {
                val ep = printerInterface.getEndpoint(i)
                if (ep.direction == UsbConstants.USB_DIR_OUT) {
                    endpointOut = ep
                    break
                }
            }

            val transferResult = connection.bulkTransfer(endpointOut, payloadBytes, payloadBytes.size, 5000)
            connection.releaseInterface(printerInterface)
            connection.close()

            if (transferResult >= 0) {
                onComplete(true, "Printed successfully to USB thermal printer.")
            } else {
                onComplete(false, "USB print transfer failed.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(false, "USB print error: ${e.localizedMessage}")
        }
    }
}
