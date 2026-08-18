package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Supplier
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose100
import com.example.ui.theme.Rose600
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SupplierScreen(viewModel: StoreViewModel) {
    val context = LocalContext.current
    val suppliers by viewModel.allSuppliers.collectAsState()
    val activeSupplier by viewModel.activeSupplierForLedger.collectAsState()
    val activeLedgers by viewModel.activeSupplierLedgers.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var supplierToEdit by remember { mutableStateOf<Supplier?>(null) }
    var supplierToDelete by remember { mutableStateOf<Supplier?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showAdjustmentDialog by remember { mutableStateOf(false) }

    val filteredSuppliers = suppliers.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.company.contains(searchQuery, ignoreCase = true)
    }

    val totalPayable = suppliers.sumOf { it.payableBalance.coerceAtLeast(0.0) }

    if (activeSupplier != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.clearSupplierLedger() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(activeSupplier!!.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Company: ${activeSupplier!!.company} • Ph: ${activeSupplier!!.phone}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                IconButton(onClick = { viewModel.exportSupplierLedgerPdf(context, activeSupplier!!, "whatsapp") }) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "WhatsApp Statement", tint = Color(0xFF25D366))
                }
                IconButton(onClick = { viewModel.exportSupplierLedgerPdf(context, activeSupplier!!, "print") }) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = "Print Statement", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { viewModel.exportSupplierLedgerPdf(context, activeSupplier!!, "share") }) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share PDF", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Rose100)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Store Payable Balance to Supplier", style = MaterialTheme.typography.labelMedium, color = Color.DarkGray)
                            Text("${settings.currencySymbol} ${activeSupplier!!.payableBalance.toInt()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Rose600)
                        }

                        Surface(
                            color = Rose600,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "STORE PAYABLE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showPaymentDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                        ) {
                            Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Make Payment", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showAdjustmentDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Payable (+)", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Supplier Ledger History (${activeLedgers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Room DB Verified", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (activeLedgers.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No supplier ledger history found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeLedgers) { ledger ->
                        val dtStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(ledger.timestamp))
                        val isPurchase = ledger.type == "PURCHASE" || ledger.type == "CREDIT_ADJUSTMENT"
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = if (isPurchase) Rose100 else Emerald100,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = if (isPurchase) "PURCHASE (+)" else "PAYMENT (-)",
                                                color = if (isPurchase) Rose600 else Emerald600,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = ledger.paymentMethod,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(ledger.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(dtStr, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (isPurchase) "+" else "-"} ${settings.currencySymbol} ${ledger.amount.toInt()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPurchase) Rose600 else Emerald600
                                    )
                                    Text("Balance: ${settings.currencySymbol} ${ledger.balanceAfter.toInt()}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { supplierToEdit = null; showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) { Icon(imageVector = Icons.Default.Add, contentDescription = "Add Supplier") }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Rose100, contentColor = Rose600)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Rose600.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Business, contentDescription = null, tint = Rose600)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Total Store Payables to Suppliers", style = MaterialTheme.typography.labelLarge, color = Color.DarkGray)
                            Text("${settings.currencySymbol} ${totalPayable.toInt()}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search supplier or company...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Suppliers & Vendors (${filteredSuppliers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredSuppliers.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No suppliers found.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSuppliers) { supplier ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.loadSupplierLedger(supplier) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(supplier.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        if (supplier.company.isNotBlank()) Text("Company: ${supplier.company}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        if (supplier.phone.isNotBlank()) Text("Ph: ${supplier.phone}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${settings.currencySymbol} ${supplier.payableBalance.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Rose600)
                                        Text("Store Payable", style = MaterialTheme.typography.labelSmall, color = Rose600)

                                        Row {
                                            IconButton(onClick = { viewModel.exportSupplierLedgerPdf(context, supplier, "whatsapp") }, modifier = Modifier.size(32.dp)) {
                                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share WhatsApp", tint = Color(0xFF25D366))
                                            }
                                            IconButton(onClick = { supplierToEdit = supplier; showAddDialog = true }, modifier = Modifier.size(32.dp)) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { supplierToDelete = supplier }, modifier = Modifier.size(32.dp)) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Rose600)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSupplierDialogModal(
            supplier = supplierToEdit,
            onSave = { supp -> viewModel.saveSupplier(supp); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }

    if (showPaymentDialog && activeSupplier != null) {
        RecordSupplierPaymentModal(
            title = "Record Payment to Supplier",
            currentBalance = activeSupplier!!.payableBalance,
            currencySymbol = settings.currencySymbol,
            onSave = { amount, notes, mode ->
                viewModel.recordSupplierPayment(activeSupplier!!.id, amount, notes, mode)
                showPaymentDialog = false
            },
            onDismiss = { showPaymentDialog = false }
        )
    }

    if (showAdjustmentDialog && activeSupplier != null) {
        AddSupplierPayableAdjustmentModal(
            supplierName = activeSupplier!!.name,
            currencySymbol = settings.currencySymbol,
            onSave = { amount, notes ->
                viewModel.addSupplierCreditAdjustment(activeSupplier!!.id, amount, notes)
                showAdjustmentDialog = false
            },
            onDismiss = { showAdjustmentDialog = false }
        )
    }

    if (supplierToDelete != null) {
        AlertDialog(
            onDismissRequest = { supplierToDelete = null },
            title = { Text("Delete Supplier") },
            text = { Text("Delete '${supplierToDelete!!.name}'?") },
            confirmButton = {
                Button(onClick = { viewModel.deleteSupplier(supplierToDelete!!); supplierToDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { supplierToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AddSupplierDialogModal(
    supplier: Supplier?,
    onSave: (Supplier) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var company by remember { mutableStateOf(supplier?.company ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var payableBalance by remember { mutableStateOf(supplier?.payableBalance?.toInt()?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (supplier == null) "Add Supplier" else "Edit Supplier") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Contact Name *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company / Brand") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = payableBalance, onValueChange = { payableBalance = it }, label = { Text("Initial Store Payable Balance") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onSave(Supplier(id = supplier?.id ?: 0L, name = name.trim(), company = company.trim(), phone = phone.trim(), address = address.trim(), payableBalance = payableBalance.toDoubleOrNull() ?: 0.0))
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RecordSupplierPaymentModal(
    title: String,
    currentBalance: Double,
    currencySymbol: String,
    onSave: (Double, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("Cash") }

    val paymentModes = listOf("Cash", "Bank Transfer", "UPI / Online", "Cheque")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Store Payable Balance: $currencySymbol ${currentBalance.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Rose600)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Payment Made Amount *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Payment Method:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    paymentModes.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notes / Voucher No. (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountInput.toDoubleOrNull()
                if (amt != null && amt > 0) onSave(amt, notesInput.trim(), selectedMode)
            }) { Text("Record Payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddSupplierPayableAdjustmentModal(
    supplierName: String,
    currencySymbol: String,
    onSave: (Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Manual Payable Charge") },
        text = {
            Column {
                Text("Adding manual payable entry for $supplierName", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Payable Amount ($currencySymbol) *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Reason / Invoice Reference *") },
                    placeholder = { Text("e.g. Credit purchase invoice, Adjustment") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountInput.toDoubleOrNull()
                if (amt != null && amt > 0) onSave(amt, notesInput.trim())
            }) { Text("Add Payable") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
