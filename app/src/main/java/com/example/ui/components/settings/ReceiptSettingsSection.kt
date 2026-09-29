package com.example.ui.components.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.StoreSettings
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.util.PosSettingsManager
import kotlinx.coroutines.launch

@Composable
fun ReceiptSettingsSection(
    settingsManager: PosSettingsManager,
    storeSettings: StoreSettings?,
    onSaveStoreSettings: ((StoreSettings) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var paperSize by remember { mutableStateOf(settingsManager.getReceiptPaperSize()) }
    var receiptHeader by remember { mutableStateOf(settingsManager.getReceiptHeader()) }
    var receiptFooter by remember { mutableStateOf(settingsManager.getReceiptFooter()) }
    var showLogo by remember { mutableStateOf(settingsManager.isShowReceiptLogo()) }
    var showStoreDetails by remember { mutableStateOf(settingsManager.isShowReceiptStoreDetails()) }
    var showDateTime by remember { mutableStateOf(settingsManager.isShowReceiptDateTime()) }
    var showTaxInfo by remember { mutableStateOf(settingsManager.isShowReceiptTaxInfo()) }
    var showItemDesc by remember { mutableStateOf(settingsManager.isShowReceiptItemDesc()) }
    var autoPrint by remember { mutableStateOf(settingsManager.isAutoPrintReceiptEnabled()) }

    var isTestPrinting by remember { mutableStateOf(false) }
    var testPrintStatus by remember { mutableStateOf<String?>(null) }
    var showSavedMessage by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(
            title = "Receipt Settings & Thermal Layout",
            subtitle = "Paper width, header/footer branding, auto-print & live preview"
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("receipt_settings_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Paper Size Selection
                Text("Thermal Paper Size", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = paperSize == 80,
                        onClick = { paperSize = 80 },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("80mm (Standard Desktop / POS)")
                            }
                        },
                        modifier = Modifier.testTag("chip_paper_80mm")
                    )
                    FilterChip(
                        selected = paperSize == 58,
                        onClick = { paperSize = 58 },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("58mm (Mobile Handheld)")
                            }
                        },
                        modifier = Modifier.testTag("chip_paper_58mm")
                    )
                }

                Divider(color = Slate200)

                // 2. Receipt Header & Footer Configuration
                OutlinedTextField(
                    value = receiptHeader,
                    onValueChange = { receiptHeader = it },
                    label = { Text("Receipt Header Note (Optional)") },
                    placeholder = { Text("e.g. Welcome to Our Store • NTN: 1234567") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("receipt_header_input")
                )

                OutlinedTextField(
                    value = receiptFooter,
                    onValueChange = { receiptFooter = it },
                    label = { Text("Receipt Footer Note / Return Policy") },
                    placeholder = { Text("e.g. Thank you for your business! No return without receipt.") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("receipt_footer_input")
                )

                Divider(color = Slate200)

                // 3. Display Toggles
                Text("Display Elements", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)

                ReceiptToggleRow(
                    title = "Print Store Logo",
                    subtitle = "Include graphical logo at the top of receipt",
                    checked = showLogo,
                    onCheckedChange = { showLogo = it },
                    testTag = "toggle_receipt_logo"
                )

                ReceiptToggleRow(
                    title = "Store Name & Contact Details",
                    subtitle = "Phone, owner name, and store address",
                    checked = showStoreDetails,
                    onCheckedChange = { showStoreDetails = it },
                    testTag = "toggle_receipt_store_details"
                )

                ReceiptToggleRow(
                    title = "Date & Time Stamp",
                    subtitle = "Transaction timestamp on receipt header",
                    checked = showDateTime,
                    onCheckedChange = { showDateTime = it },
                    testTag = "toggle_receipt_datetime"
                )

                ReceiptToggleRow(
                    title = "Tax & NTN Information",
                    subtitle = "FBR tax registration number and tax breakdown",
                    checked = showTaxInfo,
                    onCheckedChange = { showTaxInfo = it },
                    testTag = "toggle_receipt_tax_info"
                )

                ReceiptToggleRow(
                    title = "Detailed Item Description & Unit",
                    subtitle = "Show complete item name, quantity and unit rate",
                    checked = showItemDesc,
                    onCheckedChange = { showItemDesc = it },
                    testTag = "toggle_receipt_item_desc"
                )

                Divider(color = Slate200)

                // 4. Auto-Print Setting (CRITICAL: Defaults to OFF)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (autoPrint) Emerald50 else Slate100)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (autoPrint) Icons.Default.Print else Icons.Default.PrintDisabled,
                                contentDescription = null,
                                tint = if (autoPrint) Emerald700 else Slate600,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Auto-Print on Completed Sale",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (autoPrint) Emerald900 else Navy900
                            )
                        }
                        Text(
                            text = if (autoPrint) "Enabled: Receipts print automatically when a sale is finalized" else "Disabled: Manual print only (Prevents paper waste)",
                            fontSize = 11.5.sp,
                            color = if (autoPrint) Emerald800 else Slate600
                        )
                    }
                    Switch(
                        checked = autoPrint,
                        onCheckedChange = { autoPrint = it },
                        modifier = Modifier.testTag("switch_auto_print")
                    )
                }

                // 5. Actions: Save Settings & Test Print
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            settingsManager.setReceiptPaperSize(paperSize)
                            settingsManager.setReceiptHeader(receiptHeader)
                            settingsManager.setReceiptFooter(receiptFooter)
                            settingsManager.setShowReceiptLogo(showLogo)
                            settingsManager.setShowReceiptStoreDetails(showStoreDetails)
                            settingsManager.setShowReceiptDateTime(showDateTime)
                            settingsManager.setShowReceiptTaxInfo(showTaxInfo)
                            settingsManager.setShowReceiptItemDesc(showItemDesc)
                            settingsManager.setAutoPrintReceiptEnabled(autoPrint)

                            // Also sync paper width with StoreSettings
                            storeSettings?.let { current ->
                                onSaveStoreSettings?.invoke(current.copy(paperWidthMm = paperSize, invoiceFooterText = receiptFooter))
                            }

                            showSavedMessage = true
                            Toast.makeText(context, "Receipt settings saved successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        modifier = Modifier.weight(1f).testTag("btn_save_receipt_settings")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Settings", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isTestPrinting = true
                                testPrintStatus = null
                                val btMac = settingsManager.getConnectedBluetoothPrinterMac()
                                val isNetEnabled = settingsManager.isNetworkPrinterEnabled()
                                val netIp = settingsManager.getNetworkPrinterIp()
                                val netPort = settingsManager.getNetworkPrinterPort()

                                when {
                                    !btMac.isNullOrBlank() -> {
                                        val (ok, msg) = settingsManager.testPrintBluetooth(btMac, paperSize)
                                        testPrintStatus = msg
                                    }
                                    isNetEnabled && settingsManager.validateIpAddress(netIp) -> {
                                        val (ok, msg) = settingsManager.testPrintNetwork(netIp, netPort, paperSize)
                                        testPrintStatus = msg
                                    }
                                    else -> {
                                        testPrintStatus = "No Bluetooth or Network printer configured. Spooling to Android Print Preview..."
                                        Toast.makeText(context, testPrintStatus, Toast.LENGTH_LONG).show()
                                    }
                                }
                                isTestPrinting = false
                            }
                        },
                        enabled = !isTestPrinting,
                        modifier = Modifier.weight(1f).testTag("btn_test_print_receipt")
                    ) {
                        if (isTestPrinting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Print")
                        }
                    }
                }

                if (testPrintStatus != null) {
                    Text(
                        text = testPrintStatus!!,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Navy800
                    )
                }

                if (showSavedMessage) {
                    Surface(
                        color = Emerald50,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(16.dp))
                            Text("All receipt and paper configurations persisted locally.", color = Emerald800, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 6. REAL-TIME RECEIPT PREVIEW CARD
        // ==========================================
        Text("Receipt Layout Live Preview", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)

        Card(
            modifier = Modifier.fillMaxWidth().testTag("receipt_preview_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate100),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate300)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Receipt Paper Surface
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .width(if (paperSize <= 58) 280.dp else 360.dp)
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (showLogo) {
                            Surface(
                                color = Gold100,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = "[ STORE LOGO ]",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Gold700,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = storeSettings?.appDisplayName ?: "CHOUDHRY POS APP",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )

                        if (showStoreDetails) {
                            Text(
                                text = "${storeSettings?.address ?: "Main Market, Store #1"}\nPhone: ${storeSettings?.phone ?: "03080018035"}",
                                fontSize = 10.sp,
                                color = Color.DarkGray,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                lineHeight = 13.sp
                            )
                        }

                        if (receiptHeader.isNotBlank()) {
                            Text(
                                text = receiptHeader,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }

                        Text("--------------------------------", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)

                        if (showDateTime) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Date: 2026-09-26", fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                                Text("Time: 14:30:15", fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Inv #: INV-10024", fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("Cashier: Umer", fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        }

                        Text("--------------------------------", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)

                        // Sample Item Rows
                        PreviewItemRow("01 Mineral Water 1.5L", "2.0 x 80.00", "160.00", showItemDesc)
                        PreviewItemRow("02 Cooking Oil 1L", "1.0 x 480.00", "480.00", showItemDesc)

                        Text("--------------------------------", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)

                        PreviewTotalRow("Subtotal:", "640.00")
                        PreviewTotalRow("Discount:", "-20.00")
                        if (showTaxInfo) {
                            PreviewTotalRow("Tax / GST (5%):", "31.00")
                        }
                        PreviewTotalRow("TOTAL DUE (Rs):", "651.00", isBold = true)
                        PreviewTotalRow("Cash Received:", "700.00")
                        PreviewTotalRow("Change Returned:", "49.00")

                        Text("================================", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)

                        Text(
                            text = receiptFooter.ifBlank { "Thank you for shopping with us!" },
                            fontSize = 9.5.sp,
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "© 2027 CHOUDHURY POS. All Rights Reserved.",
                            fontSize = 8.5.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Navy900)
            Text(subtitle, fontSize = 11.sp, color = Slate600)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun PreviewItemRow(name: String, qtyRate: String, total: String, showDetails: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Text(total, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
        }
        if (showDetails) {
            Text(qtyRate, fontSize = 8.5.sp, color = Color.DarkGray, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun PreviewTotalRow(label: String, value: String, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 10.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, color = Color.Black, fontFamily = FontFamily.Monospace)
        Text(value, fontSize = 10.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, color = Color.Black, fontFamily = FontFamily.Monospace)
    }
}
