package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.Supplier
import com.example.ui.components.AppHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val suppliers by viewModel.suppliers.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val currency = storeSettings?.currencySymbol ?: "Rs"

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }
    var viewingLedgerSupplier by remember { mutableStateOf<Supplier?>(null) }
    var showPayoutDialogForSupplier by remember { mutableStateOf<Supplier?>(null) }

    val filteredSuppliers = remember(suppliers, searchQuery) {
        if (searchQuery.isBlank()) suppliers
        else {
            suppliers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.companyName.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // ADD / EDIT SUPPLIER DIALOG
    if (showAddDialog) {
        var name by remember { mutableStateOf(editingSupplier?.name ?: "") }
        var company by remember { mutableStateOf(editingSupplier?.companyName ?: "") }
        var phone by remember { mutableStateOf(editingSupplier?.phone ?: "") }
        var balance by remember { mutableStateOf(editingSupplier?.balance?.toString() ?: "0.0") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (editingSupplier == null) "Add Vendor / Supplier" else "Edit Vendor / Supplier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company / Distributor Name") },
                        modifier = Modifier.fillMaxWidth().testTag("supplier_company_input")
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Contact Person Name") },
                        modifier = Modifier.fillMaxWidth().testTag("supplier_name_input")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = balance,
                        onValueChange = { balance = it },
                        label = { Text("Payable Balance ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() || company.isNotBlank()) {
                            val s = (editingSupplier ?: Supplier()).copy(
                                name = name.ifBlank { company },
                                companyName = company,
                                phone = phone,
                                balance = balance.toDoubleOrNull() ?: 0.0
                            )
                            viewModel.saveSupplier(s) {
                                showAddDialog = false
                                editingSupplier = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("save_supplier_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PAYOUT SUPPLIER DIALOG
    if (showPayoutDialogForSupplier != null) {
        val supp = showPayoutDialogForSupplier!!
        var payAmount by remember { mutableStateOf("%.0f".format(supp.balance)) }
        var payMethod by remember { mutableStateOf("Cash") }
        var payNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPayoutDialogForSupplier = null },
            title = {
                Text("Make Vendor Payout / Payment", fontWeight = FontWeight.Bold, color = Navy900)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Vendor: ${supp.companyName.ifBlank { supp.name }}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Navy900
                    )
                    Text(
                        "Current Outstanding Payable: $currency %.2f".format(supp.balance),
                        fontSize = 13.sp,
                        color = Rose600,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text("Disbursement Amount ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("supplier_payout_amount_input")
                    )

                    Text("Payment Disbursed Via:", fontSize = 12.sp, color = Navy700)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Cash", "Bank", "Cheque").forEach { method ->
                            FilterChip(
                                selected = payMethod == method,
                                onClick = { payMethod = method },
                                label = { Text(method, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = payNote,
                        onValueChange = { payNote = it },
                        label = { Text("Reference / Receipt # (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = payAmount.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0) {
                            Toast.makeText(context, "Please enter valid payment amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.paySupplier(
                            supplierId = supp.id,
                            amount = amt,
                            paymentMethod = payMethod,
                            notes = payNote
                        ) {
                            showPayoutDialogForSupplier = null
                            viewingLedgerSupplier = null
                            Toast.makeText(context, "Payout of $currency %.2f recorded!".format(amt), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                    modifier = Modifier.testTag("confirm_supplier_payout_btn")
                ) {
                    Text("Disburse & Deduct Balance")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayoutDialogForSupplier = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // SUPPLIER LEDGER DIALOG
    if (viewingLedgerSupplier != null) {
        val supp = viewingLedgerSupplier!!
        val supplierPurchases = remember(purchases, supp) {
            purchases.filter { it.supplierId == supp.id }
                .sortedByDescending { it.createdAt }
        }

        Dialog(onDismissRequest = { viewingLedgerSupplier = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = supp.companyName.ifBlank { supp.name },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Navy900
                            )
                            Text(
                                text = "Contact: ${supp.name} • Phone: ${supp.phone.ifBlank { "N/A" }}",
                                fontSize = 12.sp,
                                color = Navy500
                            )
                        }
                        IconButton(onClick = { viewingLedgerSupplier = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Payable Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (supp.balance > 0) Rose100.copy(alpha = 0.5f) else Emerald100.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Outstanding Payable:", fontSize = 11.sp, color = Navy600)
                                Text(
                                    "$currency %.2f".format(supp.balance),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (supp.balance > 0) Rose700 else Emerald700
                                )
                            }
                            Button(
                                onClick = { showPayoutDialogForSupplier = supp },
                                colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Make Payment", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Purchase History & Invoices (${supplierPurchases.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Navy900
                        )

                        TextButton(
                            onClick = {
                                val statement = buildSupplierStatementText(supp, supplierPurchases, currency)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, statement)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Vendor Statement"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider(color = Slate200)

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (supplierPurchases.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No purchase bills recorded for this vendor.", color = Navy500, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(supplierPurchases, key = { it.id }) { p ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Slate50),
                                    border = BorderStroke(1.dp, Slate200)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                p.billNumber.ifBlank { "Bill #${p.id}" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Navy900
                                            )
                                            Text(
                                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(p.createdAt)),
                                                fontSize = 10.sp,
                                                color = Navy500
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Total: $currency %.0f".format(p.totalAmount), fontSize = 11.sp, color = Navy700)
                                            Text("Paid: $currency %.0f".format(p.paidAmount), fontSize = 11.sp, color = Emerald600)
                                            if (p.dueAmount > 0) {
                                                Text("Due: $currency %.0f".format(p.dueAmount), fontSize = 11.sp, color = Rose600, fontWeight = FontWeight.Bold)
                                            } else {
                                                Text("Cleared", fontSize = 11.sp, color = Emerald600)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                editingSupplier = supp
                                showAddDialog = true
                                viewingLedgerSupplier = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Details", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewingLedgerSupplier = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // MAIN VENDOR LIST
    Scaffold(
        topBar = {
            AppHeader(
                title = "Suppliers & Vendor Accounts",
                subtitle = "${suppliers.size} Registered Vendors",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSupplier = null
                    showAddDialog = true
                },
                containerColor = Navy900,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_supplier_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Supplier")
            }
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search supplier name, company or phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Navy900,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredSuppliers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No suppliers registered.", color = Navy500)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSuppliers, key = { it.id }) { supplier ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewingLedgerSupplier = supplier
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = supplier.companyName.ifBlank { supplier.name },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Navy900
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Contact: ${supplier.name} • Phone: ${supplier.phone.ifBlank { "N/A" }}",
                                        fontSize = 12.sp,
                                        color = Navy500
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    StatusBadge(
                                        text = if (supplier.balance <= 0) "Clear" else "Payable: $currency %.0f".format(supplier.balance),
                                        backgroundColor = if (supplier.balance <= 0) Emerald100 else Rose100,
                                        textColor = if (supplier.balance <= 0) Emerald600 else Rose600
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { showPayoutDialogForSupplier = supplier },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Payments, contentDescription = "Pay Vendor", tint = Rose600)
                                        }
                                        IconButton(
                                            onClick = {
                                                editingSupplier = supplier
                                                showAddDialog = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Navy700)
                                        }
                                        IconButton(
                                            onClick = { viewModel.softDeleteSupplier(supplier.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Rose600)
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

private fun buildSupplierStatementText(supp: Supplier, purchases: List<com.example.data.entity.Purchase>, currency: String): String {
    val df = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
    val sb = StringBuilder()
    sb.appendLine("========================================")
    sb.appendLine("            VENDOR STATEMENT            ")
    sb.appendLine("========================================")
    sb.appendLine("Vendor : ${supp.companyName.ifBlank { supp.name }}")
    sb.appendLine("Contact: ${supp.name}")
    sb.appendLine("Phone  : ${supp.phone.ifBlank { "N/A" }}")
    sb.appendLine("Date   : ${df.format(Date())}")
    sb.appendLine("----------------------------------------")
    sb.appendLine("Outstanding Payable: $currency ${"%.2f".format(supp.balance)}")
    sb.appendLine("Total Purchase Bills: ${purchases.size}")
    sb.appendLine("----------------------------------------")
    purchases.take(15).forEach { p ->
        sb.appendLine("${p.billNumber.ifBlank { "Bill #${p.id}" }} | ${df.format(Date(p.createdAt))}")
        sb.appendLine("  Total: $currency ${"%.0f".format(p.totalAmount)} | Paid: $currency ${"%.0f".format(p.paidAmount)} | Due: $currency ${"%.0f".format(p.dueAmount)}")
    }
    sb.appendLine("========================================")
    return sb.toString()
}
