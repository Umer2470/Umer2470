package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose100
import com.example.ui.theme.Rose600
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.BatchImportParser

@Composable
fun BatchImportModal(
    viewModel: StoreViewModel,
    onDismiss: () -> Unit
) {
    val parsedItems by viewModel.parsedBatchItems.collectAsState()
    val isParsing by viewModel.isParsingBatch.collectAsState()
    val isImporting by viewModel.isImportingBatch.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var rawInputText by remember { mutableStateOf("") }
    var addToExistingStock by remember { mutableStateOf(true) }
    var updatePrices by remember { mutableStateOf(true) }
    var showFormatInfo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!isImporting) {
                viewModel.clearBatchParsedItems()
                onDismiss()
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Batch Stock Import", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                IconButton(onClick = {
                    viewModel.clearBatchParsedItems()
                    onDismiss()
                }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (parsedItems.isEmpty()) {
                    // STAGE 1: TEXT INPUT MODE
                    Text(
                        text = "Import new shipments or bulk products by pasting CSV or JSON data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                rawInputText = BatchImportParser.DEMO_SHIPMENT_CSV
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load Demo Data", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { showFormatInfo = !showFormatInfo },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showFormatInfo) "Hide Format" else "View Format", fontSize = 11.sp)
                        }
                    }

                    if (showFormatInfo) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = BatchImportParser.SAMPLE_CSV_HEADER_INFO,
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rawInputText,
                        onValueChange = { rawInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 220.dp),
                        placeholder = {
                            Text(
                                "Paste CSV or JSON list of products here...\n\nExample:\nCPVC Pipe 1 inch, Plumbing, 220, 280, 40, Pcs, 8901011",
                                fontSize = 12.sp
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    )

                    if (rawInputText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { rawInputText = "" }) {
                                Text("Clear Text", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                } else {
                    // STAGE 2: PREVIEW & VERIFICATION MODE
                    val existingCount = parsedItems.count { it.isExisting }
                    val newCount = parsedItems.count { !it.isExisting }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Found ${parsedItems.size} Valid Products",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "$newCount New Products • $existingCount Existing Stock Match",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Import Settings Options
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = addToExistingStock,
                                onCheckedChange = { addToExistingStock = it }
                            )
                            Text("Add imported quantity to existing stock", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = updatePrices,
                                onCheckedChange = { updatePrices = it }
                            )
                            Text("Update purchase & sale prices for existing items", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Shipment Items Preview:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(parsedItems) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (item.isExisting) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = CircleShape
                                                ) {
                                                    Text(
                                                        if (addToExistingStock) "STOCK ADD (+${item.stockQuantity.toInt()})" else "STOCK REPLACE",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else {
                                                Surface(
                                                    color = Emerald100,
                                                    shape = CircleShape
                                                ) {
                                                    Text(
                                                        "NEW PRODUCT",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Emerald600,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(item.category, fontSize = 10.sp, color = Color.Gray)
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                        if (item.barcode.isNotBlank()) {
                                            Text("Barcode: ${item.barcode}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "+${item.stockQuantity.toInt()} ${item.unit}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        if (item.isExisting && addToExistingStock) {
                                            Text(
                                                text = "New Total: ${(item.currentStock + item.stockQuantity).toInt()} ${item.unit}",
                                                fontSize = 10.sp,
                                                color = Emerald600,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = "Buy: ${settings.currencySymbol}${item.purchasePrice.toInt()} • Sell: ${settings.currencySymbol}${item.salePrice.toInt()}",
                                            fontSize = 10.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (parsedItems.isEmpty()) {
                Button(
                    onClick = {
                        if (rawInputText.isNotBlank()) {
                            viewModel.parseBatchImportText(rawInputText)
                        }
                    },
                    enabled = rawInputText.isNotBlank() && !isParsing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isParsing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Parsing Input...")
                    } else {
                        Text("Parse & Preview Shipment")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearBatchParsedItems() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back to Edit")
                    }

                    Button(
                        onClick = {
                            viewModel.executeBatchImport(
                                items = parsedItems,
                                addToExistingStock = addToExistingStock,
                                updatePrices = updatePrices
                            ) {
                                onDismiss()
                            }
                        },
                        enabled = !isImporting,
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importing...")
                        } else {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Confirm Import")
                        }
                    }
                }
            }
        },
        dismissButton = null
    )
}
