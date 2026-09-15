package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.Customer
import com.example.data.entity.Sale
import com.example.ui.components.AppHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val currency = storeSettings?.currencySymbol ?: "Rs"

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var viewingLedgerCustomer by remember { mutableStateOf<Customer?>(null) }
    var showPaymentDialogForCustomer by remember { mutableStateOf<Customer?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else {
            customers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // ADD / EDIT CUSTOMER DIALOG
    if (showAddDialog) {
        var name by remember { mutableStateOf(editingCustomer?.name ?: "") }
        var phone by remember { mutableStateOf(editingCustomer?.phone ?: "") }
        var address by remember { mutableStateOf(editingCustomer?.address ?: "") }
        var balance by remember { mutableStateOf(editingCustomer?.balance?.toString() ?: "0.0") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (editingCustomer == null) "Add Customer" else "Edit Customer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth().testTag("customer_name_input")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = balance,
                        onValueChange = { balance = it },
                        label = { Text("Opening Balance / Current Due ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val c = (editingCustomer ?: Customer()).copy(
                                name = name,
                                phone = phone,
                                address = address,
                                balance = balance.toDoubleOrNull() ?: 0.0
                            )
                            viewModel.saveCustomer(c) {
                                showAddDialog = false
                                editingCustomer = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("save_customer_button")
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

    // RECEIVE CUSTOMER PAYMENT DIALOG
    if (showPaymentDialogForCustomer != null) {
        val cust = showPaymentDialogForCustomer!!
        var payAmount by remember { mutableStateOf("%.0f".format(cust.balance)) }
        var payMethod by remember { mutableStateOf("Cash") }
        var payNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPaymentDialogForCustomer = null },
            title = {
                Text("Receive Customer Payment", fontWeight = FontWeight.Bold, color = Navy900)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Customer: ${cust.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                    Text("Outstanding Balance: $currency %.2f".format(cust.balance), fontSize = 13.sp, color = Rose600, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text("Payment Amount ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("receive_payment_amount_input")
                    )

                    Text("Payment Method:", fontSize = 12.sp, color = Navy700)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Cash", "Bank", "Easypaisa", "JazzCash").forEach { method ->
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
                        label = { Text("Payment Note / Reference (Optional)") },
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
                        viewModel.receiveCustomerPayment(
                            customerId = cust.id,
                            amount = amt,
                            paymentMethod = payMethod,
                            notes = payNote
                        ) {
                            showPaymentDialogForCustomer = null
                            viewingLedgerCustomer = null
                            Toast.makeText(context, "Payment of $currency %.2f received!".format(amt), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    modifier = Modifier.testTag("confirm_receive_payment_btn")
                ) {
                    Text("Receive & Update Balance")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialogForCustomer = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // CUSTOMER LEDGER / STATEMENT DIALOG
    if (viewingLedgerCustomer != null) {
        val cust = viewingLedgerCustomer!!
        val customerInvoices = remember(sales, cust) {
            sales.filter { it.customerId == cust.id || it.customerName.equals(cust.name, ignoreCase = true) }
                .sortedByDescending { it.createdAt }
        }

        Dialog(onDismissRequest = { viewingLedgerCustomer = null }) {
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
                                text = cust.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Navy900
                            )
                            Text(
                                text = "Phone: ${cust.phone.ifBlank { "N/A" }}",
                                fontSize = 12.sp,
                                color = Navy500
                            )
                        }
                        IconButton(onClick = { viewingLedgerCustomer = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Balance Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = if (cust.balance > 0) Rose100.copy(alpha = 0.5f) else Emerald100.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Current Outstanding Due:", fontSize = 11.sp, color = Navy600)
                                Text(
                                    "$currency %.2f".format(cust.balance),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (cust.balance > 0) Rose700 else Emerald700
                                )
                            }
                            Button(
                                onClick = { showPaymentDialogForCustomer = cust },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Receive Payment", fontSize = 12.sp)
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
                            "Account Statement & Invoices (${customerInvoices.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Navy900
                        )

                        TextButton(
                            onClick = {
                                val statementText = buildCustomerStatementText(cust, customerInvoices, currency)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, statementText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Customer Statement"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share Statement", fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider(color = Slate200)

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (customerInvoices.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No previous transactions recorded.", color = Navy500, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(customerInvoices, key = { it.id }) { sale ->
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
                                                sale.invoiceNumber,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Navy900
                                            )
                                            Text(
                                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(sale.createdAt)),
                                                fontSize = 10.sp,
                                                color = Navy500
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Net Total: $currency %.0f".format(sale.netAmount), fontSize = 11.sp, color = Navy700)
                                            Text("Paid: $currency %.0f".format(sale.paidAmount), fontSize = 11.sp, color = Emerald600, fontWeight = FontWeight.SemiBold)
                                            if (sale.dueAmount > 0) {
                                                Text("Due: $currency %.0f".format(sale.dueAmount), fontSize = 11.sp, color = Rose600, fontWeight = FontWeight.Bold)
                                            } else {
                                                Text("Fully Paid", fontSize = 11.sp, color = Emerald600)
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
                                editingCustomer = cust
                                showAddDialog = true
                                viewingLedgerCustomer = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Info", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewingLedgerCustomer = null },
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

    // MAIN CUSTOMER LIST
    Scaffold(
        topBar = {
            AppHeader(
                title = "Customer Accounts & Ledger",
                subtitle = "${customers.size} Registered Accounts",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCustomer = null
                    showAddDialog = true
                },
                containerColor = Navy900,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
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
                placeholder = { Text("Search customer name or phone...") },
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

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No customers found.", color = Navy500)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewingLedgerCustomer = customer
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
                                        text = customer.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Navy900
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Phone: ${customer.phone.ifBlank { "N/A" }}",
                                        fontSize = 12.sp,
                                        color = Navy500
                                    )
                                    if (customer.address.isNotBlank()) {
                                        Text(
                                            text = "Address: ${customer.address}",
                                            fontSize = 11.sp,
                                            color = Navy600
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    StatusBadge(
                                        text = if (customer.balance <= 0) "Balance: 0" else "Due: $currency %.0f".format(customer.balance),
                                        backgroundColor = if (customer.balance <= 0) Emerald100 else Rose100,
                                        textColor = if (customer.balance <= 0) Emerald600 else Rose600
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { showPaymentDialogForCustomer = customer },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Payment, contentDescription = "Receive Payment", tint = Emerald600)
                                        }
                                        IconButton(
                                            onClick = {
                                                editingCustomer = customer
                                                showAddDialog = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Navy700)
                                        }
                                        IconButton(
                                            onClick = { viewModel.softDeleteCustomer(customer.id) },
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

private fun buildCustomerStatementText(cust: Customer, invoices: List<Sale>, currency: String): String {
    val df = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
    val totalPurchases = invoices.sumOf { it.netAmount }
    val totalPaid = invoices.sumOf { it.paidAmount }

    val sb = StringBuilder()
    sb.appendLine("========================================")
    sb.appendLine("           CUSTOMER STATEMENT           ")
    sb.appendLine("========================================")
    sb.appendLine("Customer: ${cust.name}")
    sb.appendLine("Phone   : ${cust.phone.ifBlank { "N/A" }}")
    sb.appendLine("Date    : ${df.format(Date())}")
    sb.appendLine("----------------------------------------")
    sb.appendLine("Current Outstanding Balance: $currency ${"%.2f".format(cust.balance)}")
    sb.appendLine("Lifetime Invoices Total    : $currency ${"%.2f".format(totalPurchases)}")
    sb.appendLine("Lifetime Total Paid        : $currency ${"%.2f".format(totalPaid)}")
    sb.appendLine("----------------------------------------")
    sb.appendLine("TRANSACTION HISTORY:")
    invoices.take(15).forEach { inv ->
        sb.appendLine("${inv.invoiceNumber} | ${df.format(Date(inv.createdAt))}")
        sb.appendLine("  Net: $currency ${"%.0f".format(inv.netAmount)} | Paid: $currency ${"%.0f".format(inv.paidAmount)} | Due: $currency ${"%.0f".format(inv.dueAmount)}")
    }
    sb.appendLine("========================================")
    return sb.toString()
}
