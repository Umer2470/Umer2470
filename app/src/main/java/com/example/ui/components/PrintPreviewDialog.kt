package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import com.example.ui.theme.BentoCardDark
import com.example.ui.theme.BentoEmerald
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryLight
import com.example.util.EscPosThermalPrinterService
import com.example.util.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PrintPreviewDialog(
    sale: Sale,
    items: List<SaleItem>,
    settings: StoreSettings,
    initialFormat: PdfGenerator.ReceiptFormat = PdfGenerator.ReceiptFormat.THERMAL_58MM,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Format selection: 0 = 58mm Thermal, 1 = 80mm Thermal, 2 = A4
    var selectedFormatIndex by remember {
        mutableIntStateOf(
            when (initialFormat) {
                PdfGenerator.ReceiptFormat.THERMAL_58MM -> 0
                PdfGenerator.ReceiptFormat.THERMAL_80MM -> 1
                PdfGenerator.ReceiptFormat.A4 -> 2
            }
        )
    }

    val currentFormat = when (selectedFormatIndex) {
        0 -> PdfGenerator.ReceiptFormat.THERMAL_58MM
        1 -> PdfGenerator.ReceiptFormat.THERMAL_80MM
        else -> PdfGenerator.ReceiptFormat.A4
    }

    // PDF Preview state
    var renderedBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isGeneratingPdf by remember { mutableStateOf(true) }
    var pdfFileState by remember { mutableStateOf<File?>(null) }

    // Bluetooth Printer state
    var pairedDevices by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var selectedDeviceAddress by remember { mutableStateOf<String?>(null) }
    var selectedDeviceName by remember { mutableStateOf<String>("Default Bluetooth Printer") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var isPrinting by remember { mutableStateOf(false) }

    // Load paired Bluetooth devices
    fun refreshBluetoothDevices() {
        val devices = EscPosThermalPrinterService.getPairedDevices(context)
        pairedDevices = devices
        if (devices.isNotEmpty() && selectedDeviceAddress == null) {
            val autoSelected = devices.find {
                it.first.contains("Printer", ignoreCase = true) ||
                it.first.contains("POS", ignoreCase = true) ||
                it.first.contains("Thermal", ignoreCase = true) ||
                it.first.contains("RP", ignoreCase = true)
            } ?: devices.first()
            selectedDeviceAddress = autoSelected.second
            selectedDeviceName = autoSelected.first
        }
    }

    LaunchedEffect(Unit) {
        refreshBluetoothDevices()
    }

    // Regenerate PDF & rendered page bitmaps whenever format changes
    LaunchedEffect(selectedFormatIndex) {
        isGeneratingPdf = true
        withContext(Dispatchers.IO) {
            val pdfFile = PdfGenerator.generatePrintablePdfInvoice(
                context = context,
                sale = sale,
                items = items,
                settings = settings,
                format = currentFormat
            )
            pdfFileState = pdfFile

            if (pdfFile != null && pdfFile.exists()) {
                val bitmaps = renderPdfFileToBitmaps(pdfFile)
                withContext(Dispatchers.Main) {
                    renderedBitmaps = bitmaps
                    isGeneratingPdf = false
                }
            } else {
                withContext(Dispatchers.Main) {
                    renderedBitmaps = emptyList()
                    isGeneratingPdf = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize(0.95f)
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BentoPrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = BentoPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Print Preview & Thermal Print",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BentoCardDark
                            )
                            Text(
                                text = "Invoice #: ${sale.invoiceNumber} • ${sale.customerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Preview")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Format Selector Tabs
                TabRow(
                    selectedTabIndex = selectedFormatIndex,
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = BentoPrimary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedFormatIndex == 0,
                        onClick = { selectedFormatIndex = 0 },
                        text = { Text("58mm Thermal", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedFormatIndex == 1,
                        onClick = { selectedFormatIndex = 1 },
                        text = { Text("80mm Thermal", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedFormatIndex == 2,
                        onClick = { selectedFormatIndex = 2 },
                        text = { Text("A4 Document", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Center Print Preview Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE2E8F0))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGeneratingPdf) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BentoPrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Rendering High-Precision PDF Preview...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BentoCardDark
                            )
                        }
                    } else if (renderedBitmaps.isEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Unable to render PDF preview",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(renderedBitmaps) { index, bitmap ->
                                Card(
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier
                                        .width(
                                            when (selectedFormatIndex) {
                                                0 -> 260.dp
                                                1 -> 320.dp
                                                else -> 380.dp
                                            }
                                        )
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "PDF Invoice Page ${index + 1}",
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Page ${index + 1} of ${renderedBitmaps.size} (${currentFormat.name})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bluetooth Printer Control Panel Card
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BluetoothConnected,
                                    contentDescription = null,
                                    tint = BentoPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Connected Bluetooth Printer:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { refreshBluetoothDevices() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Bluetooth devices",
                                    tint = BentoPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Device Selection Dropdown Menu Trigger
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { isDropdownExpanded = true }
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp)),
                                color = Color.White
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = selectedDeviceName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BentoCardDark
                                        )
                                        if (!selectedDeviceAddress.isNullOrBlank()) {
                                            Text(
                                                text = "MAC: ${selectedDeviceAddress}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Text("Change ▾", style = MaterialTheme.typography.labelSmall, color = BentoPrimary, fontWeight = FontWeight.Bold)
                                }
                            }

                            DropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false }
                            ) {
                                if (pairedDevices.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No paired Bluetooth devices found") },
                                        onClick = { isDropdownExpanded = false }
                                    )
                                } else {
                                    pairedDevices.forEach { device ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(device.first, fontWeight = FontWeight.Bold)
                                                    Text(device.second, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                }
                                            },
                                            onClick = {
                                                selectedDeviceName = device.first
                                                selectedDeviceAddress = device.second
                                                isDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Send to Bluetooth Thermal Printer Button
                    Button(
                        onClick = {
                            isPrinting = true
                            val paperFmt = if (selectedFormatIndex == 1) EscPosThermalPrinterService.PaperFormat.FORMAT_80MM else EscPosThermalPrinterService.PaperFormat.FORMAT_58MM
                            val escPosBytes = EscPosThermalPrinterService.generateEscPosCommandBytes(sale, items, settings, paperFmt)

                            EscPosThermalPrinterService.printViaBluetooth(
                                context = context,
                                payloadBytes = escPosBytes,
                                deviceAddress = selectedDeviceAddress
                            ) { success, msg ->
                                isPrinting = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isPrinting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Transmitting to Thermal Printer...", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send to Thermal Printer (Bluetooth)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary Actions (USB Print, System Print, Share PDF)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val paperFmt = if (selectedFormatIndex == 1) EscPosThermalPrinterService.PaperFormat.FORMAT_80MM else EscPosThermalPrinterService.PaperFormat.FORMAT_58MM
                                val escPosBytes = EscPosThermalPrinterService.generateEscPosCommandBytes(sale, items, settings, paperFmt)
                                EscPosThermalPrinterService.printViaUsb(context, escPosBytes) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("USB Print", style = MaterialTheme.typography.bodySmall)
                        }

                        OutlinedButton(
                            onClick = {
                                pdfFileState?.let { file ->
                                    PdfGenerator.printPdf(context, file)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Android Print", style = MaterialTheme.typography.bodySmall)
                        }

                        OutlinedButton(
                            onClick = {
                                pdfFileState?.let { file ->
                                    PdfGenerator.openOrSharePdf(context, file, action = "share")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share PDF", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Utility function to render pages of a PDF File into Bitmaps using Android's native PdfRenderer
 */
private fun renderPdfFileToBitmaps(pdfFile: File, dpi: Int = 144): List<Bitmap> {
    val bitmaps = mutableListOf<Bitmap>()
    try {
        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(pfd)
        val pageCount = pdfRenderer.pageCount
        val scale = dpi / 72f

        for (i in 0 until pageCount) {
            val page = pdfRenderer.openPage(i)
            val width = (page.width * scale).toInt()
            val height = (page.height * scale).toInt()

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(AndroidColor.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmaps.add(bitmap)
            page.close()
        }
        pdfRenderer.close()
        pfd.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return bitmaps
}
