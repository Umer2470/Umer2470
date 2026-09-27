package com.example.ui.components.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.util.PosSettingsManager
import com.example.util.PrinterConnectionStatus
import kotlinx.coroutines.launch

@Composable
fun NetworkPrinterSection(
    settingsManager: PosSettingsManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var ipAddress by remember { mutableStateOf(settingsManager.getNetworkPrinterIp()) }
    var portString by remember { mutableStateOf(settingsManager.getNetworkPrinterPort().toString()) }
    var isEnabled by remember { mutableStateOf(settingsManager.isNetworkPrinterEnabled()) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var testResultStatus by remember { mutableStateOf<PrinterConnectionStatus?>(null) }
    var ipError by remember { mutableStateOf<String?>(null) }
    var portError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(
            title = "Network (LAN / Wi-Fi) Thermal Printer",
            subtitle = "Direct TCP/IP socket printing over local Ethernet or Wi-Fi"
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("network_printer_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Connection Status Banner
                Surface(
                    color = if (isEnabled && ipAddress.isNotBlank()) {
                        when (testResultStatus) {
                            PrinterConnectionStatus.CONNECTED -> Emerald50
                            PrinterConnectionStatus.CONNECTING -> Amber50
                            PrinterConnectionStatus.FAILED -> Rose50
                            else -> Blue50
                        }
                    } else Slate100,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = if (isEnabled && ipAddress.isNotBlank()) {
                                when (testResultStatus) {
                                    PrinterConnectionStatus.CONNECTED -> Emerald600
                                    PrinterConnectionStatus.CONNECTING -> Amber600
                                    PrinterConnectionStatus.FAILED -> Rose600
                                    else -> Blue600
                                }
                            } else Slate500,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lan,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnabled && ipAddress.isNotBlank()) "Network Printer: $ipAddress:${portString.ifBlank { "9100" }}" else "Network Printer: Not Configured",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Navy900
                            )
                            Text(
                                text = when {
                                    !isEnabled -> "Status: Disabled (Enable switch below to activate)"
                                    testResultStatus == PrinterConnectionStatus.CONNECTED -> "Status: Online & Ready (TCP Socket Verified)"
                                    testResultStatus == PrinterConnectionStatus.CONNECTING -> "Status: Connecting to printer host..."
                                    testResultStatus == PrinterConnectionStatus.FAILED -> "Status: Unreachable (Check Wi-Fi network & IP)"
                                    ipAddress.isNotBlank() -> "Status: Configured (Tap 'Test Connection' to verify)"
                                    else -> "Status: Enter IP address and port to connect"
                                },
                                fontSize = 11.5.sp,
                                color = when (testResultStatus) {
                                    PrinterConnectionStatus.CONNECTED -> Emerald700
                                    PrinterConnectionStatus.FAILED -> Rose700
                                    else -> Slate600
                                }
                            )
                        }
                    }
                }

                // 2. Enable / Disable Network Printer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Network Printing", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                        Text("Send receipts over local Wi-Fi or LAN connection", fontSize = 11.5.sp, color = Slate600)
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        modifier = Modifier.testTag("switch_network_printer_enabled")
                    )
                }

                Divider(color = Slate200)

                // 3. IP Address & Port Input
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = {
                        ipAddress = it
                        ipError = null
                    },
                    label = { Text("Printer IP Address (IPv4)") },
                    placeholder = { Text("e.g. 192.168.1.100") },
                    singleLine = true,
                    isError = ipError != null,
                    supportingText = {
                        if (ipError != null) {
                            Text(ipError!!, color = Rose600)
                        } else {
                            Text("Static IP assigned to printer on store local router", fontSize = 11.sp, color = Slate600)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("net_printer_ip_input")
                )

                OutlinedTextField(
                    value = portString,
                    onValueChange = {
                        portString = it
                        portError = null
                    },
                    label = { Text("TCP Port (Standard: 9100)") },
                    placeholder = { Text("9100") },
                    singleLine = true,
                    isError = portError != null,
                    supportingText = {
                        if (portError != null) {
                            Text(portError!!, color = Rose600)
                        } else {
                            Text("Most ESC/POS network thermal printers listen on port 9100", fontSize = 11.sp, color = Slate600)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("net_printer_port_input")
                )

                // 4. Action Buttons: Save & Test Print
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val cleanIp = ipAddress.trim()
                            val cleanPort = portString.trim().toIntOrNull() ?: 9100

                            var valid = true
                            if (cleanIp.isNotBlank() && !settingsManager.validateIpAddress(cleanIp)) {
                                ipError = "Invalid IPv4 format (e.g. 192.168.1.100)"
                                valid = false
                            }
                            if (!settingsManager.validatePort(cleanPort)) {
                                portError = "Port must be between 1 and 65535"
                                valid = false
                            }

                            if (valid) {
                                settingsManager.saveNetworkPrinterConfig(cleanIp, cleanPort, isEnabled)
                                statusMessage = "Network printer settings saved successfully!"
                                Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        modifier = Modifier.weight(1f).testTag("btn_save_network_printer")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Config", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val cleanIp = ipAddress.trim()
                            val cleanPort = portString.trim().toIntOrNull() ?: 9100

                            if (!settingsManager.validateIpAddress(cleanIp)) {
                                ipError = "Enter valid IP address to test"
                                return@OutlinedButton
                            }
                            if (!settingsManager.validatePort(cleanPort)) {
                                portError = "Enter valid port (1-65535)"
                                return@OutlinedButton
                            }

                            scope.launch {
                                isTestingConnection = true
                                testResultStatus = PrinterConnectionStatus.CONNECTING
                                statusMessage = "Connecting to $cleanIp:$cleanPort..."

                                val (success, msg) = settingsManager.testPrintNetwork(cleanIp, cleanPort, settingsManager.getReceiptPaperSize())
                                isTestingConnection = false
                                statusMessage = msg
                                testResultStatus = if (success) PrinterConnectionStatus.CONNECTED else PrinterConnectionStatus.FAILED
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isTestingConnection && ipAddress.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("btn_test_network_printer")
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Print")
                        }
                    }
                }

                if (statusMessage != null) {
                    Text(
                        text = statusMessage!!,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (testResultStatus == PrinterConnectionStatus.CONNECTED) Emerald800 else Navy800
                    )
                }
            }
        }
    }
}
