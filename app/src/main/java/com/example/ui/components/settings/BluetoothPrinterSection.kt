package com.example.ui.components.settings

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.util.PosSettingsManager
import com.example.util.PrinterConnectionStatus
import kotlinx.coroutines.launch

@Composable
fun BluetoothPrinterSection(
    settingsManager: PosSettingsManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var connectedMac by remember { mutableStateOf(settingsManager.getConnectedBluetoothPrinterMac()) }
    var connectedName by remember { mutableStateOf(settingsManager.getConnectedBluetoothPrinterName()) }
    var discoveredDevices by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf(if (connectedMac.isNullOrBlank()) PrinterConnectionStatus.DISCONNECTED else PrinterConnectionStatus.CONNECTED) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isTestPrinting by remember { mutableStateOf(false) }

    val bluetoothAdapter: BluetoothAdapter? = remember { BluetoothAdapter.getDefaultAdapter() }

    // Bluetooth Runtime Permission Handling for Android 12+ (API 31+)
    val hasBluetoothPermissions = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.values.all { it }
        hasBluetoothPermissions.value = allGranted
        if (allGranted) {
            scanPrinters(
                settingsManager = settingsManager,
                onScanStart = { isScanning = true },
                onScanResult = {
                    discoveredDevices = it
                    isScanning = false
                }
            )
        } else {
            statusMessage = "Bluetooth permission is required to scan and connect to thermal printers."
            Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(
            title = "Bluetooth Thermal Printer",
            subtitle = "Wireless ESC/POS thermal printing for receipts and invoices"
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("bluetooth_printer_card"),
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
                    color = when (connectionStatus) {
                        PrinterConnectionStatus.CONNECTED -> Emerald50
                        PrinterConnectionStatus.CONNECTING -> Amber50
                        PrinterConnectionStatus.SCANNING -> Blue50
                        PrinterConnectionStatus.FAILED -> Rose50
                        PrinterConnectionStatus.DISCONNECTED -> Slate100
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = when (connectionStatus) {
                                PrinterConnectionStatus.CONNECTED -> Emerald600
                                PrinterConnectionStatus.CONNECTING -> Amber600
                                PrinterConnectionStatus.SCANNING -> Blue600
                                PrinterConnectionStatus.FAILED -> Rose600
                                PrinterConnectionStatus.DISCONNECTED -> Slate500
                            },
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (connectionStatus == PrinterConnectionStatus.CONNECTED) Icons.Default.Print else Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (!connectedMac.isNullOrBlank()) "Connected Printer: ${connectedName ?: connectedMac}" else "Connected Printer: None",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Navy900
                            )
                            Text(
                                text = when (connectionStatus) {
                                    PrinterConnectionStatus.CONNECTED -> "Status: Ready to Print (ESC/POS Wireless Stream)"
                                    PrinterConnectionStatus.CONNECTING -> "Status: Connecting to printer hardware..."
                                    PrinterConnectionStatus.SCANNING -> "Status: Scanning for nearby Bluetooth devices..."
                                    PrinterConnectionStatus.FAILED -> "Status: Connection Failed (Check power & distance)"
                                    PrinterConnectionStatus.DISCONNECTED -> "Status: No printer connected"
                                },
                                fontSize = 11.5.sp,
                                color = when (connectionStatus) {
                                    PrinterConnectionStatus.CONNECTED -> Emerald700
                                    PrinterConnectionStatus.CONNECTING -> Amber700
                                    PrinterConnectionStatus.FAILED -> Rose700
                                    else -> Slate600
                                }
                            )
                        }

                        if (!connectedMac.isNullOrBlank()) {
                            IconButton(
                                onClick = {
                                    settingsManager.disconnectBluetoothPrinter()
                                    connectedMac = null
                                    connectedName = null
                                    connectionStatus = PrinterConnectionStatus.DISCONNECTED
                                    statusMessage = "Printer disconnected."
                                },
                                modifier = Modifier.size(32.dp).testTag("btn_disconnect_bluetooth_printer")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = Rose600)
                            }
                        }
                    }
                }

                if (bluetoothAdapter == null) {
                    Surface(
                        color = Amber50,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Bluetooth hardware is not available on this device.",
                            color = Amber800,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else if (!bluetoothAdapter.isEnabled) {
                    Surface(
                        color = Amber50,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.BluetoothDisabled, contentDescription = null, tint = Amber700)
                            Text(
                                text = "Bluetooth is currently turned OFF. Please enable Bluetooth in your device settings to discover printers.",
                                color = Amber800,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // 2. Action Buttons: Scan & Test Print
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermissions.value) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_CONNECT,
                                        Manifest.permission.BLUETOOTH_SCAN
                                    )
                                )
                            } else {
                                isScanning = true
                                connectionStatus = PrinterConnectionStatus.SCANNING
                                scanPrinters(
                                    settingsManager = settingsManager,
                                    onScanStart = { isScanning = true },
                                    onScanResult = {
                                        discoveredDevices = it
                                        isScanning = false
                                        connectionStatus = if (!connectedMac.isNullOrBlank()) PrinterConnectionStatus.CONNECTED else PrinterConnectionStatus.DISCONNECTED
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        enabled = !isScanning,
                        modifier = Modifier.weight(1f).testTag("btn_scan_printers")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scanning...")
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan for Printers", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val targetMac = connectedMac
                            if (targetMac.isNullOrBlank()) {
                                Toast.makeText(context, "No printer connected. Please connect a printer first.", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            scope.launch {
                                isTestPrinting = true
                                statusMessage = "Sending test receipt to $connectedName..."
                                val (success, msg) = settingsManager.testPrintBluetooth(targetMac, settingsManager.getReceiptPaperSize())
                                isTestPrinting = false
                                statusMessage = msg
                                connectionStatus = if (success) PrinterConnectionStatus.CONNECTED else PrinterConnectionStatus.FAILED
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !connectedMac.isNullOrBlank() && !isTestPrinting,
                        modifier = Modifier.weight(1f).testTag("btn_test_print_bluetooth")
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

                if (statusMessage != null) {
                    Text(
                        text = statusMessage!!,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Navy800
                    )
                }

                // 3. Discovered / Paired Printers List
                if (discoveredDevices.isNotEmpty()) {
                    Text(
                        text = "Available & Paired Printers (${discoveredDevices.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Navy900
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        discoveredDevices.forEach { (address, name) ->
                            val isThisConnected = address == connectedMac
                            Surface(
                                color = if (isThisConnected) Emerald50 else Slate50,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isThisConnected) Emerald400 else Slate300
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                                        Text("MAC: $address", fontSize = 11.sp, color = Slate600)
                                    }

                                    if (isThisConnected) {
                                        Surface(
                                            color = Emerald600,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                Text("Connected", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                settingsManager.setConnectedBluetoothPrinter(address, name)
                                                connectedMac = address
                                                connectedName = name
                                                connectionStatus = PrinterConnectionStatus.CONNECTED
                                                statusMessage = "Connected to $name ($address)"
                                                Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("btn_connect_device_$address")
                                        ) {
                                            Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (!isScanning) {
                    Surface(
                        color = Slate50,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "No printers discovered in current scan",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Navy800
                            )
                            Text(
                                text = "Pair your ESC/POS thermal printer in Android Settings > Bluetooth, then tap 'Scan for Printers' to connect.",
                                fontSize = 11.sp,
                                color = Slate600,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun scanPrinters(
    settingsManager: PosSettingsManager,
    onScanStart: () -> Unit,
    onScanResult: (List<Pair<String, String>>) -> Unit
) {
    onScanStart()
    val paired = settingsManager.getBondedBluetoothPrinters()
    onScanResult(paired)
}
