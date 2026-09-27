package com.example.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

/**
 * Data model for submitted Feature Requests
 */
data class FeatureRequestItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val contactInfo: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Connection states for Bluetooth and Network Printers
 */
enum class PrinterConnectionStatus {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    FAILED
}

/**
 * Comprehensive Manager for:
 * 1. Support & Portal Configuration
 * 2. Receipt Layout & Auto-Print Settings
 * 3. Invoice PDF Settings
 * 4. Bluetooth Thermal Printer
 * 5. Network (TCP/IP ESC/POS) Printer
 */
class PosSettingsManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ==========================================
    // 1. SUPPORT & PORTAL CONFIGURATION
    // ==========================================
    fun getSupportEmail(): String = prefs.getString(KEY_SUPPORT_EMAIL, "sentrystore.pk@gmail.com") ?: "sentrystore.pk@gmail.com"
    fun setSupportEmail(email: String) = prefs.edit().putString(KEY_SUPPORT_EMAIL, email.trim()).apply()

    fun getSupportWhatsapp(): String = prefs.getString(KEY_SUPPORT_WHATSAPP, "03080018035") ?: "03080018035"
    fun setSupportWhatsapp(number: String) = prefs.edit().putString(KEY_SUPPORT_WHATSAPP, number.trim()).apply()

    fun getPortalUrl(): String = prefs.getString(KEY_PORTAL_URL, "") ?: ""
    fun setPortalUrl(url: String) = prefs.edit().putString(KEY_PORTAL_URL, url.trim()).apply()

    fun getIosDownloadUrl(): String = prefs.getString(KEY_IOS_DOWNLOAD_URL, "") ?: ""
    fun setIosDownloadUrl(url: String) = prefs.edit().putString(KEY_IOS_DOWNLOAD_URL, url.trim()).apply()

    fun getWindowsDownloadUrl(): String = prefs.getString(KEY_WINDOWS_DOWNLOAD_URL, "") ?: ""
    fun setWindowsDownloadUrl(url: String) = prefs.edit().putString(KEY_WINDOWS_DOWNLOAD_URL, url.trim()).apply()

    fun getTermsOfServiceUrl(): String = prefs.getString(KEY_TOS_URL, "") ?: ""
    fun setTermsOfServiceUrl(url: String) = prefs.edit().putString(KEY_TOS_URL, url.trim()).apply()

    // Feature Requests
    fun saveFeatureRequest(request: FeatureRequestItem) {
        val list = getFeatureRequests().toMutableList()
        list.add(0, request)
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("description", item.description)
                put("contactInfo", item.contactInfo)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_FEATURE_REQUESTS, jsonArray.toString()).apply()
    }

    fun getFeatureRequests(): List<FeatureRequestItem> {
        val jsonString = prefs.getString(KEY_FEATURE_REQUESTS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<FeatureRequestItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    FeatureRequestItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        contactInfo = obj.optString("contactInfo", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // 2. RECEIPT SETTINGS
    // ==========================================
    fun getReceiptPaperSize(): Int = prefs.getInt(KEY_RECEIPT_PAPER_SIZE, 80)
    fun setReceiptPaperSize(size: Int) = prefs.edit().putInt(KEY_RECEIPT_PAPER_SIZE, if (size <= 58) 58 else 80).apply()

    fun getReceiptHeader(): String = prefs.getString(KEY_RECEIPT_HEADER, "") ?: ""
    fun setReceiptHeader(header: String) = prefs.edit().putString(KEY_RECEIPT_HEADER, header.trim()).apply()

    fun getReceiptFooter(): String = prefs.getString(KEY_RECEIPT_FOOTER, "Thank you for your business! No return without receipt.") ?: ""
    fun setReceiptFooter(footer: String) = prefs.edit().putString(KEY_RECEIPT_FOOTER, footer.trim()).apply()

    fun isShowReceiptLogo(): Boolean = prefs.getBoolean(KEY_SHOW_RECEIPT_LOGO, true)
    fun setShowReceiptLogo(show: Boolean) = prefs.edit().putBoolean(KEY_SHOW_RECEIPT_LOGO, show).apply()

    fun isShowReceiptStoreDetails(): Boolean = prefs.getBoolean(KEY_SHOW_RECEIPT_STORE_DETAILS, true)
    fun setShowReceiptStoreDetails(show: Boolean) = prefs.edit().putBoolean(KEY_SHOW_RECEIPT_STORE_DETAILS, show).apply()

    fun isShowReceiptDateTime(): Boolean = prefs.getBoolean(KEY_SHOW_RECEIPT_DATETIME, true)
    fun setShowReceiptDateTime(show: Boolean) = prefs.edit().putBoolean(KEY_SHOW_RECEIPT_DATETIME, show).apply()

    fun isShowReceiptTaxInfo(): Boolean = prefs.getBoolean(KEY_SHOW_RECEIPT_TAX_INFO, true)
    fun setShowReceiptTaxInfo(show: Boolean) = prefs.edit().putBoolean(KEY_SHOW_RECEIPT_TAX_INFO, show).apply()

    fun isShowReceiptItemDesc(): Boolean = prefs.getBoolean(KEY_SHOW_RECEIPT_ITEM_DESC, true)
    fun setShowReceiptItemDesc(show: Boolean) = prefs.edit().putBoolean(KEY_SHOW_RECEIPT_ITEM_DESC, show).apply()

    // Auto-Print: STRICT RULE — Must default to OFF (false) unless explicitly enabled
    fun isAutoPrintReceiptEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_PRINT_RECEIPT, false)
    fun setAutoPrintReceiptEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_PRINT_RECEIPT, enabled).apply()

    // ==========================================
    // 3. INVOICE PDF SETTINGS
    // ==========================================
    fun getPdfTagline(): String = prefs.getString(KEY_PDF_TAGLINE, "Professional Retail & Business Management") ?: "Professional Retail & Business Management"
    fun setPdfTagline(tagline: String) = prefs.edit().putString(KEY_PDF_TAGLINE, tagline.trim()).apply()

    fun getPdfAccentColor(): String = prefs.getString(KEY_PDF_ACCENT_COLOR, "Navy") ?: "Navy"
    fun setPdfAccentColor(colorName: String) = prefs.edit().putString(KEY_PDF_ACCENT_COLOR, colorName).apply()

    fun getPdfFooterText(): String = prefs.getString(KEY_PDF_FOOTER_TEXT, "Thank you for shopping with us! Please retain this invoice for warranty and tax purposes.") ?: ""
    fun setPdfFooterText(text: String) = prefs.edit().putString(KEY_PDF_FOOTER_TEXT, text.trim()).apply()

    fun isPdfShowContactInfo(): Boolean = prefs.getBoolean(KEY_PDF_SHOW_CONTACT, true)
    fun setPdfShowContactInfo(show: Boolean) = prefs.edit().putBoolean(KEY_PDF_SHOW_CONTACT, show).apply()

    fun isPdfShowAddress(): Boolean = prefs.getBoolean(KEY_PDF_SHOW_ADDRESS, true)
    fun setPdfShowAddress(show: Boolean) = prefs.edit().putBoolean(KEY_PDF_SHOW_ADDRESS, show).apply()

    fun isPdfShowTaxReg(): Boolean = prefs.getBoolean(KEY_PDF_SHOW_TAX_REG, true)
    fun setPdfShowTaxReg(show: Boolean) = prefs.edit().putBoolean(KEY_PDF_SHOW_TAX_REG, show).apply()

    fun getPdfTaxRegNumber(): String = prefs.getString(KEY_PDF_TAX_REG_NUMBER, "") ?: ""
    fun setPdfTaxRegNumber(reg: String) = prefs.edit().putString(KEY_PDF_TAX_REG_NUMBER, reg.trim()).apply()

    // ==========================================
    // 4. BLUETOOTH PRINTER CONFIGURATION
    // ==========================================
    private val _bluetoothStatus = MutableStateFlow(PrinterConnectionStatus.DISCONNECTED)
    val bluetoothStatus: StateFlow<PrinterConnectionStatus> = _bluetoothStatus.asStateFlow()

    fun getConnectedBluetoothPrinterName(): String? = prefs.getString(KEY_BT_PRINTER_NAME, null)
    fun getConnectedBluetoothPrinterMac(): String? = prefs.getString(KEY_BT_PRINTER_MAC, null)

    fun setConnectedBluetoothPrinter(mac: String?, name: String?) {
        prefs.edit().apply {
            if (mac.isNullOrBlank()) {
                remove(KEY_BT_PRINTER_MAC)
                remove(KEY_BT_PRINTER_NAME)
                _bluetoothStatus.value = PrinterConnectionStatus.DISCONNECTED
            } else {
                putString(KEY_BT_PRINTER_MAC, mac.trim())
                putString(KEY_BT_PRINTER_NAME, name?.trim() ?: "Thermal Printer")
                _bluetoothStatus.value = PrinterConnectionStatus.CONNECTED
            }
            apply()
        }
        // Sync with legacy EscPosThermalPrinterService config
        EscPosThermalPrinterService.savePrinterConfig(context, mac, name, getReceiptPaperSize())
    }

    fun disconnectBluetoothPrinter() {
        setConnectedBluetoothPrinter(null, null)
        _bluetoothStatus.value = PrinterConnectionStatus.DISCONNECTED
    }

    @SuppressLint("MissingPermission")
    fun getBondedBluetoothPrinters(): List<Pair<String, String>> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            if (!adapter.isEnabled) return emptyList()
            adapter.bondedDevices?.map { device ->
                Pair(device.address, device.name ?: "Bluetooth Printer (${device.address})")
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun testPrintBluetooth(macAddress: String, paperWidthMm: Int = 80): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            return@withContext Pair(false, "Bluetooth is disabled or not supported on this device.")
        }

        val testInvoiceText = buildTestReceiptString(paperWidthMm)
        val textBytes = testInvoiceText.toByteArray(Charsets.UTF_8)
        val initCmd = byteArrayOf(0x1B, 0x40) // ESC @
        val cutCmd = byteArrayOf(0x0A, 0x0A, 0x1D, 0x56, 0x42, 0x00) // GS V B 0
        val payload = initCmd + textBytes + cutCmd

        var socket: android.bluetooth.BluetoothSocket? = null
        var outputStream: OutputStream? = null
        try {
            _bluetoothStatus.value = PrinterConnectionStatus.CONNECTING
            val device = adapter.getRemoteDevice(macAddress)
            val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket.connect()
            outputStream = socket.outputStream
            outputStream.write(payload)
            outputStream.flush()
            _bluetoothStatus.value = PrinterConnectionStatus.CONNECTED
            Pair(true, "Test receipt printed successfully via Bluetooth!")
        } catch (e: Exception) {
            _bluetoothStatus.value = PrinterConnectionStatus.FAILED
            Pair(false, "Failed to connect to printer: ${e.localizedMessage ?: "Connection error"}")
        } finally {
            try { outputStream?.close() } catch (ignored: Exception) {}
            try { socket?.close() } catch (ignored: Exception) {}
        }
    }

    // ==========================================
    // 5. NETWORK PRINTER CONFIGURATION
    // ==========================================
    private val _networkStatus = MutableStateFlow(PrinterConnectionStatus.DISCONNECTED)
    val networkStatus: StateFlow<PrinterConnectionStatus> = _networkStatus.asStateFlow()

    fun getNetworkPrinterIp(): String = prefs.getString(KEY_NET_PRINTER_IP, "") ?: ""
    fun getNetworkPrinterPort(): Int = prefs.getInt(KEY_NET_PRINTER_PORT, 9100)
    fun isNetworkPrinterEnabled(): Boolean = prefs.getBoolean(KEY_NET_PRINTER_ENABLED, false)

    fun saveNetworkPrinterConfig(ip: String, port: Int, enabled: Boolean) {
        prefs.edit()
            .putString(KEY_NET_PRINTER_IP, ip.trim())
            .putInt(KEY_NET_PRINTER_PORT, port)
            .putBoolean(KEY_NET_PRINTER_ENABLED, enabled)
            .apply()
    }

    fun validateIpAddress(ip: String): Boolean {
        val clean = ip.trim()
        if (clean.isBlank()) return false
        val parts = clean.split(".")
        if (parts.size != 4) return false
        return parts.all {
            val num = it.toIntOrNull()
            num != null && num in 0..255
        }
    }

    fun validatePort(port: Int): Boolean = port in 1..65535

    suspend fun testPrintNetwork(ip: String, port: Int, paperWidthMm: Int = 80): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!validateIpAddress(ip)) {
            return@withContext Pair(false, "Invalid IP Address format (e.g. 192.168.1.100).")
        }
        if (!validatePort(port)) {
            return@withContext Pair(false, "Invalid Port number (1-65535, standard: 9100).")
        }

        val testInvoiceText = buildTestReceiptString(paperWidthMm)
        val textBytes = testInvoiceText.toByteArray(Charsets.UTF_8)
        val initCmd = byteArrayOf(0x1B, 0x40) // ESC @
        val cutCmd = byteArrayOf(0x0A, 0x0A, 0x1D, 0x56, 0x42, 0x00) // GS V B 0
        val payload = initCmd + textBytes + cutCmd

        var socket: Socket? = null
        try {
            _networkStatus.value = PrinterConnectionStatus.CONNECTING
            socket = Socket()
            socket.connect(InetSocketAddress(ip.trim(), port), 3500)
            socket.soTimeout = 3500
            val out = socket.getOutputStream()
            out.write(payload)
            out.flush()
            _networkStatus.value = PrinterConnectionStatus.CONNECTED
            Pair(true, "Test receipt printed successfully via Network Printer ($ip:$port)!")
        } catch (e: Exception) {
            _networkStatus.value = PrinterConnectionStatus.FAILED
            Pair(false, "Network printer unavailable at $ip:$port (${e.localizedMessage ?: "Timeout"})")
        } finally {
            try { socket?.close() } catch (ignored: Exception) {}
        }
    }

    // ==========================================
    // 6. UNIFIED RECEIPT & AUTO-PRINT DISPATCHER
    // ==========================================
    /**
     * Dispatches receipt print job to the preferred printer:
     * 1. Bluetooth printer if connected/saved
     * 2. Network printer if configured and enabled
     * 3. Spool to Android PrintManager
     * Non-destructive: Never cancels sale or throws unhandled exceptions.
     */
    suspend fun printSaleReceipt(
        sale: Sale,
        items: List<SaleItem>,
        settings: StoreSettings
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val paperWidth = getReceiptPaperSize()
        val invoice = InvoiceFormattingService.formatSaleTransaction(sale, items, settings)
        val escPosBytes = EscPosThermalPrinterService.generateEscPosCommandBytes(invoice, paperWidth)

        // 1. Try Bluetooth Printer if configured
        val btMac = getConnectedBluetoothPrinterMac()
        if (!btMac.isNullOrBlank()) {
            val service = EscPosThermalPrinterService(context)
            val success = service.printReceipt(btMac, invoice, paperWidth)
            if (success) {
                return@withContext Pair(true, "Receipt printed via Bluetooth (${getConnectedBluetoothPrinterName() ?: btMac})")
            }
        }

        // 2. Try Network Printer if configured and enabled
        if (isNetworkPrinterEnabled() && validateIpAddress(getNetworkPrinterIp())) {
            val netIp = getNetworkPrinterIp()
            val netPort = getNetworkPrinterPort()
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(netIp, netPort), 3000)
                socket.soTimeout = 3000
                val out = socket.getOutputStream()
                out.write(escPosBytes)
                out.flush()
                return@withContext Pair(true, "Receipt printed via Network Printer ($netIp:$netPort)")
            } catch (e: Exception) {
                // Fallback to spooler
            } finally {
                try { socket?.close() } catch (ignored: Exception) {}
            }
        }

        // 3. Fallback: Android PrintManager thermal spooler
        withContext(Dispatchers.Main) {
            val thermalPdf = PdfGenerator.generateInvoicePdf(
                context,
                invoice,
                if (paperWidth <= 58) PdfGenerator.ReceiptFormat.THERMAL_80MM else PdfGenerator.ReceiptFormat.THERMAL_80MM
            )
            if (thermalPdf != null) {
                PdfGenerator.printPdfFile(context, thermalPdf, "Receipt_${sale.invoiceNumber}")
            }
        }

        Pair(true, "Receipt spooled to Android Print Service (#${sale.invoiceNumber})")
    }

    private fun buildTestReceiptString(paperWidthMm: Int): String {
        val cols = if (paperWidthMm <= 58) 32 else 48
        val divider = "-".repeat(cols)
        val doubleDivider = "=".repeat(cols)
        val storeTitle = "CHOUDHRY POS APP"
        val padding = ((cols - storeTitle.length) / 2).coerceAtLeast(0)
        val centeredTitle = " ".repeat(padding) + storeTitle

        return buildString {
            appendLine(doubleDivider)
            appendLine(centeredTitle)
            appendLine(" ".repeat(((cols - 20) / 2).coerceAtLeast(0)) + "TEST RECEIPT PRINT")
            appendLine(divider)
            appendLine("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
            appendLine("Paper Size: ${paperWidthMm}mm  |  Columns: $cols")
            appendLine("Connection: ESC/POS Thermal Stream")
            appendLine(divider)
            appendLine("ITEM                    QTY    RATE   TOTAL")
            appendLine(divider)
            appendLine("01 Sample Product A      1.0  100.00  100.00")
            appendLine("02 Sample Product B      2.0   50.00  100.00")
            appendLine(divider)
            appendLine("SUBTOTAL:                             200.00")
            appendLine("DISCOUNT:                              10.00")
            appendLine("NET TOTAL (Rs):                       190.00")
            appendLine(doubleDivider)
            appendLine("    *** PRINTER TEST SUCCESSFUL ***")
            appendLine("© 2026 Quickro. All rights reserved.")
            appendLine("\n\n")
        }
    }

    companion object {
        private const val PREFS_NAME = "choudhry_pos_custom_settings"

        // Support & Portal
        private const val KEY_SUPPORT_EMAIL = "key_support_email"
        private const val KEY_SUPPORT_WHATSAPP = "key_support_whatsapp"
        private const val KEY_PORTAL_URL = "key_portal_url"
        private const val KEY_IOS_DOWNLOAD_URL = "key_ios_download_url"
        private const val KEY_WINDOWS_DOWNLOAD_URL = "key_windows_download_url"
        private const val KEY_TOS_URL = "key_tos_url"
        private const val KEY_FEATURE_REQUESTS = "key_feature_requests"

        // Receipt Settings
        private const val KEY_RECEIPT_PAPER_SIZE = "key_receipt_paper_size"
        private const val KEY_RECEIPT_HEADER = "key_receipt_header"
        private const val KEY_RECEIPT_FOOTER = "key_receipt_footer"
        private const val KEY_SHOW_RECEIPT_LOGO = "key_show_receipt_logo"
        private const val KEY_SHOW_RECEIPT_STORE_DETAILS = "key_show_receipt_store_details"
        private const val KEY_SHOW_RECEIPT_DATETIME = "key_show_receipt_datetime"
        private const val KEY_SHOW_RECEIPT_TAX_INFO = "key_show_receipt_tax_info"
        private const val KEY_SHOW_RECEIPT_ITEM_DESC = "key_show_receipt_item_desc"
        private const val KEY_AUTO_PRINT_RECEIPT = "key_auto_print_receipt"

        // Invoice PDF Settings
        private const val KEY_PDF_TAGLINE = "key_pdf_tagline"
        private const val KEY_PDF_ACCENT_COLOR = "key_pdf_accent_color"
        private const val KEY_PDF_FOOTER_TEXT = "key_pdf_footer_text"
        private const val KEY_PDF_SHOW_CONTACT = "key_pdf_show_contact"
        private const val KEY_PDF_SHOW_ADDRESS = "key_pdf_show_address"
        private const val KEY_PDF_SHOW_TAX_REG = "key_pdf_show_tax_reg"
        private const val KEY_PDF_TAX_REG_NUMBER = "key_pdf_tax_reg_number"

        // Bluetooth Printer
        private const val KEY_BT_PRINTER_NAME = "key_bt_printer_name"
        private const val KEY_BT_PRINTER_MAC = "key_bt_printer_mac"

        // Network Printer
        private const val KEY_NET_PRINTER_IP = "key_net_printer_ip"
        private const val KEY_NET_PRINTER_PORT = "key_net_printer_port"
        private const val KEY_NET_PRINTER_ENABLED = "key_net_printer_enabled"

        @Volatile
        private var INSTANCE: PosSettingsManager? = null

        fun getInstance(context: Context): PosSettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PosSettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
