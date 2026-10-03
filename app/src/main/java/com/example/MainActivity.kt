package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CustomerEntity
import com.example.data.entity.InvoiceEntity
import com.example.data.entity.ProductEntity
import com.example.ui.calculator.StoreCalculatorDialog
import com.example.ui.sync.SharedAccountDialog
import com.example.ui.theme.ChoudhuryPosTheme
import com.example.ui.viewmodel.StoreViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: StoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.data?.scheme == "choudhurypos") {
            Toast.makeText(this, "Opened via CHOUDHURY POS Web App Link", Toast.LENGTH_SHORT).show()
        }
        setContent {
            ChoudhuryPosTheme {
                MainAppScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: StoreViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showSettlementDialog by remember { mutableStateOf(false) }
    var invoiceToSettle by remember { mutableStateOf<InvoiceEntity?>(null) }
    var settlementAmountInput by remember { mutableStateOf("") }
    var showWalkingCustomerWarning by remember { mutableStateOf(false) }
    var showCalculatorDialog by remember { mutableStateOf(false) }
    var showSharedAccountDialog by remember { mutableStateOf(false) }

    val cart by viewModel.cart.collectAsState()
    val invoiceTotal by viewModel.invoiceTotal.collectAsState()
    val paymentCalc by viewModel.paymentCalculation.collectAsState()
    val lastInvoice by viewModel.lastCompletedInvoice.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()
    val successMsg by viewModel.successMessage.collectAsState()
    val currentStoreId by viewModel.currentStoreId.collectAsState()
    val isUrdu by viewModel.isUrdu.collectAsState()

    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.errorMessage.value = null
        }
    }

    LaunchedEffect(successMsg) {
        successMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.successMessage.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isUrdu) "چودھری پوائنٹ آف سیل" else "CHOUDHURY STORE POS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Branch 0$currentStoreId",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showCalculatorDialog = true }) {
                        Icon(Icons.Default.Calculate, contentDescription = "Store Calculator")
                    }
                    IconButton(onClick = { showSharedAccountDialog = true }) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Cloud Account & Sync")
                    }
                    TextButton(onClick = { viewModel.isUrdu.value = !isUrdu }) {
                        Text(if (isUrdu) "English" else "اردو")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "POS") },
                    label = { Text(if (isUrdu) "پی او ایس" else "POS") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Receipt, contentDescription = "Invoices") },
                    label = { Text(if (isUrdu) "رسیدیں" else "Invoices") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
                    label = { Text(if (isUrdu) "گاہک" else "Customers") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                    label = { Text(if (isUrdu) "رپورٹس" else "Reports") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(if (isUrdu) "سیٹنگز" else "Settings") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> PosScreen(
                    viewModel = viewModel,
                    onOpenCheckout = { showCheckoutDialog = true },
                    onOpenCalculator = { showCalculatorDialog = true }
                )
                1 -> InvoicesScreen(viewModel = viewModel, onSettlePayment = { inv ->
                    invoiceToSettle = inv
                    settlementAmountInput = inv.balanceDue.toString()
                    showSettlementDialog = true
                })
                2 -> CustomersScreen(viewModel = viewModel)
                3 -> ReportsScreen(viewModel = viewModel)
                4 -> SettingsScreen(
                    viewModel = viewModel,
                    onOpenCalculator = { showCalculatorDialog = true },
                    onOpenCloudSync = { showSharedAccountDialog = true }
                )
            }
        }
    }

    if (showCheckoutDialog) {
        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = { Text(if (isUrdu) "ادائیگی اور بل فائنل کریں" else "Payment & Checkout", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Invoice Total (Net Payable):", fontSize = 12.sp)
                            Text("Rs ${String.format("%.2f", invoiceTotal)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    var inputAmount by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = {
                            inputAmount = it
                            viewModel.amountReceivedInput.value = it
                        },
                        label = { Text(if (isUrdu) "وصول شدہ نقد رقم (Cash Received)" else "Cash Tendered (Rs)") },
                        modifier = Modifier.fillMaxWidth().testTag("cash_received_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                inputAmount = invoiceTotal.toString()
                                viewModel.amountReceivedInput.value = inputAmount
                            },
                            modifier = Modifier.weight(1f).testTag("quick_cash_exact"),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) { Text("Exact", fontSize = 11.sp) }
                        Button(
                            onClick = {
                                val rounded = (kotlin.math.ceil(invoiceTotal / 500.0) * 500.0).coerceAtLeast(invoiceTotal)
                                inputAmount = rounded.toString()
                                viewModel.amountReceivedInput.value = inputAmount
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) { Text("+500", fontSize = 11.sp) }
                        Button(
                            onClick = {
                                val rounded = (kotlin.math.ceil(invoiceTotal / 1000.0) * 1000.0).coerceAtLeast(invoiceTotal)
                                inputAmount = rounded.toString()
                                viewModel.amountReceivedInput.value = inputAmount
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) { Text("+1000", fontSize = 11.sp) }
                        Button(
                            onClick = {
                                inputAmount = "5000"
                                viewModel.amountReceivedInput.value = inputAmount
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) { Text("5000", fontSize = 11.sp) }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount Applied:", fontSize = 13.sp)
                        Text("Rs ${String.format("%.2f", paymentCalc.amountApplied)}", fontWeight = FontWeight.SemiBold)
                    }

                    if (paymentCalc.changeDue > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Change to Return (واپسی):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            Text("Rs ${String.format("%.2f", paymentCalc.changeDue)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                    }

                    if (paymentCalc.balanceDue > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Remaining Due (ادھار):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            Text("Rs ${String.format("%.2f", paymentCalc.balanceDue)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = if (paymentCalc.status.name == "PAID") Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = paymentCalc.status.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (paymentCalc.status.name == "PAID") Color(0xFF166534) else Color(0xFFB45309)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val calc = paymentCalc
                        val customer = viewModel.selectedCustomer.value
                        if (calc.balanceDue > 0.001 && customer == null) {
                            showWalkingCustomerWarning = true
                        } else {
                            viewModel.completeCheckout(allowWalkingCustomerCredit = false) {
                                showCheckoutDialog = false
                                showReceiptDialog = true
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_checkout_btn")
                ) { Text(if (isUrdu) "بل مکمل کریں (Complete)" else "Complete Sale") }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showWalkingCustomerWarning) {
        AlertDialog(
            onDismissRequest = { showWalkingCustomerWarning = false },
            title = { Text("Confirm Walking Customer Credit", fontWeight = FontWeight.Bold) },
            text = { Text("You are creating a credit sale for a Walking Customer with unpaid balance of Rs ${paymentCalc.balanceDue}. Proceed?") },
            confirmButton = {
                Button(
                    onClick = {
                        showWalkingCustomerWarning = false
                        viewModel.completeCheckout(allowWalkingCustomerCredit = true) {
                            showCheckoutDialog = false
                            showReceiptDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Allow Credit") }
            },
            dismissButton = {
                TextButton(onClick = { showWalkingCustomerWarning = false }) { Text("Select Customer First") }
            }
        )
    }

    if (showSettlementDialog && invoiceToSettle != null) {
        val inv = invoiceToSettle!!
        AlertDialog(
            onDismissRequest = { showSettlementDialog = false },
            title = { Text("Record Payment for ${inv.invoiceNo}") },
            text = {
                Column {
                    Text("Invoice Total: Rs ${inv.totalAmount}", fontSize = 13.sp)
                    Text("Already Paid: Rs ${inv.amountApplied}", fontSize = 13.sp)
                    Text("Outstanding Due: Rs ${inv.balanceDue}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = settlementAmountInput,
                        onValueChange = { settlementAmountInput = it },
                        label = { Text("Payment Amount (Rs)") },
                        modifier = Modifier.fillMaxWidth().testTag("settlement_amount_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = settlementAmountInput.toDoubleOrNull() ?: 0.0
                        viewModel.recordLaterPayment(inv, amount) { ok ->
                            if (ok) showSettlementDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_settlement_btn")
                ) { Text("Save Payment") }
            },
            dismissButton = {
                TextButton(onClick = { showSettlementDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showReceiptDialog && lastInvoice != null) {
        val inv = lastInvoice!!
        AlertDialog(
            onDismissRequest = { showReceiptDialog = false },
            title = { Text("CHOUDHURY STORE POS — Official Receipt", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text("Invoice #: ${inv.invoiceNo}", fontWeight = FontWeight.Bold)
                    Text("Customer: ${inv.customerName}")
                    Text("Payment Method: ${inv.paymentMethod}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Invoice Total:", fontWeight = FontWeight.SemiBold)
                        Text("Rs ${String.format("%.2f", inv.totalAmount)}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cash Received:")
                        Text("Rs ${String.format("%.2f", inv.amountReceived)}")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount Applied:")
                        Text("Rs ${String.format("%.2f", inv.amountApplied)}")
                    }
                    if (inv.changeDue > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Change Returned:", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            Text("Rs ${String.format("%.2f", inv.changeDue)}", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                    }
                    if (inv.balanceDue > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Balance Due (ادھار):", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            Text("Rs ${String.format("%.2f", inv.balanceDue)}", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Status: ${inv.paymentStatus}", fontWeight = FontWeight.Bold, color = if (inv.paymentStatus == "PAID") Color(0xFF166534) else Color(0xFFB45309))
                }
            },
            confirmButton = {
                Button(onClick = { showReceiptDialog = false }) { Text("Close Receipt") }
            }
        )
    }

    if (showCalculatorDialog) {
        StoreCalculatorDialog(
            isUrdu = isUrdu,
            onDismiss = { showCalculatorDialog = false },
            onApplyToPos = { amount ->
                viewModel.amountReceivedInput.value = String.format(java.util.Locale.US, "%.2f", amount)
                viewModel.successMessage.value = "Transferred Rs ${String.format(java.util.Locale.US, "%.2f", amount)} from Calculator"
                showCalculatorDialog = false
                if (cart.isNotEmpty()) {
                    showCheckoutDialog = true
                }
            }
        )
    }

    if (showSharedAccountDialog) {
        SharedAccountDialog(
            viewModel = viewModel,
            onDismiss = { showSharedAccountDialog = false }
        )
    }
}

@Composable
fun PosScreen(
    viewModel: StoreViewModel,
    onOpenCheckout: () -> Unit,
    onOpenCalculator: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val invoiceTotal by viewModel.invoiceTotal.collectAsState()
    var search by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = {
                    search = it
                    viewModel.searchQuery.value = it
                },
                label = { Text("Search product name or barcode...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.weight(1f).testTag("pos_search_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedIconButton(
                onClick = onOpenCalculator,
                modifier = Modifier.testTag("pos_quick_calc_icon_btn")
            ) {
                Icon(Icons.Default.Calculate, contentDescription = "Quick Store Calculator", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products) { prod ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.addToCart(prod) }.testTag("product_item_${prod.id}"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(prod.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Rs ${prod.salePrice}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text("Stock: ${prod.stockQuantity} ${prod.unit}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("${cart.size} items in cart", fontSize = 12.sp)
                    Text("Total: Rs ${String.format("%.2f", invoiceTotal)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenCalculator,
                        modifier = Modifier.testTag("pos_calc_btn"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("🧮 Calc")
                    }
                    Button(
                        onClick = onOpenCheckout,
                        enabled = cart.isNotEmpty(),
                        modifier = Modifier.testTag("checkout_btn")
                    ) { Text("Checkout") }
                }
            }
        }
    }
}

@Composable
fun InvoicesScreen(viewModel: StoreViewModel, onSettlePayment: (InvoiceEntity) -> Unit) {
    val invoices by viewModel.invoices.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        items(invoices) { inv ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(inv.invoiceNo, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (inv.paymentStatus == "PAID") Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                        ) {
                            Text(
                                inv.paymentStatus,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (inv.paymentStatus == "PAID") Color(0xFF166534) else Color(0xFFB45309)
                            )
                        }
                    }
                    Text("Customer: ${inv.customerName}", fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total: Rs ${inv.totalAmount}")
                        Text("Applied: Rs ${inv.amountApplied}")
                        if (inv.balanceDue > 0.0) {
                            Text("Due: Rs ${inv.balanceDue}", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (inv.balanceDue > 0.0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { onSettlePayment(inv) },
                            modifier = Modifier.align(Alignment.End).testTag("settle_invoice_${inv.id}")
                        ) { Text("Settle Payment", fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomersScreen(viewModel: StoreViewModel) {
    val customers by viewModel.customers.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        items(customers) { c ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(c.name, fontWeight = FontWeight.Bold)
                        Text(c.phone, fontSize = 12.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Current Balance:", fontSize = 11.sp)
                        Text(
                            "Rs ${c.currentBalance}",
                            fontWeight = FontWeight.Bold,
                            color = if (c.currentBalance > 0.0) Color.Red else Color.Green
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsScreen(viewModel: StoreViewModel) {
    val invoices by viewModel.invoices.collectAsState()
    val totalRevenue = invoices.sumOf { it.totalAmount }
    val totalCashReceived = invoices.sumOf { it.amountReceived }
    val totalChangeReturned = invoices.sumOf { it.changeDue }
    val netCashRetained = totalCashReceived - totalChangeReturned
    val totalReceivables = invoices.sumOf { it.balanceDue }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Store Financial Summary", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        ReportKpiCard("Total Sales Revenue", "Rs ${String.format("%.2f", totalRevenue)}", "Net invoice sales total, excluding customer overpayments", MaterialTheme.colorScheme.primary)
        ReportKpiCard("Net Cash in Drawer", "Rs ${String.format("%.2f", netCashRetained)}", "Cash received minus change returned", Color(0xFF16A34A))
        ReportKpiCard("Customer Receivables (ادھار)", "Rs ${String.format("%.2f", totalReceivables)}", "Unpaid customer balance", Color(0xFFDC2626))
    }
}

@Composable
fun ReportKpiCard(title: String, value: String, description: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 13.sp, color = Color.Gray)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(description, fontSize = 11.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: StoreViewModel,
    onOpenCalculator: () -> Unit,
    onOpenCloudSync: () -> Unit
) {
    val context = LocalContext.current
    val currentStoreId by viewModel.currentStoreId.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Store Settings & Multi-Branch", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.switchStore(1L) },
                colors = ButtonDefaults.buttonColors(containerColor = if (currentStoreId == 1L) MaterialTheme.colorScheme.primary else Color.Gray)
            ) { Text("Store 1 (Main)") }
            Button(
                onClick = { viewModel.switchStore(2L) },
                colors = ButtonDefaults.buttonColors(containerColor = if (currentStoreId == 2L) MaterialTheme.colorScheme.primary else Color.Gray)
            ) { Text("Store 2 (Branch 2)") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Store Tools & Utilities / تجارتی اوزار", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onOpenCalculator,
            modifier = Modifier.fillMaxWidth().testTag("store_open_calculator_btn")
        ) {
            Icon(Icons.Default.Calculate, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Store Commercial Calculator (تجارتی کیلکولیٹر)")
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedButton(
            onClick = onOpenCloudSync,
            modifier = Modifier.fillMaxWidth().testTag("store_open_sync_btn")
        ) {
            Icon(Icons.Default.CloudSync, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cloud Shared Account & Multi-Branch Sync")
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Platform & Downloads / پلیٹ فارم اور ڈاؤن لوڈز", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ais-dev-repstbphrkqk34xvfwxoji-454250663559.asia-east1.run.app"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) { Text("🌐 Open Web POS Portal") }

        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ais-dev-repstbphrkqk34xvfwxoji-454250663559.asia-east1.run.app/downloads/choudhury-pos-app.apk"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) { Text("📱 Download Android APK") }

        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ais-dev-repstbphrkqk34xvfwxoji-454250663559.asia-east1.run.app/downloads/choudhury-pos-windows-x64.zip"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) { Text("💻 Download Windows Desktop App") }
    }
}
