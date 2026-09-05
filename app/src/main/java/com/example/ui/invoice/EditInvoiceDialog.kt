package com.example.ui.invoice

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.Customer
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInvoiceDialog(
    sale: Sale,
    items: List<SaleItem>,
    currency: String,
    customersList: List<Customer> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Sale) -> Unit,
    onDeleteRequest: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    var customerName by remember { mutableStateOf(sale.customerName) }
    var selectedCustomerId by remember { mutableStateOf(sale.customerId) }
    var paymentType by remember { mutableStateOf(sale.paymentType) }
    var cashierName by remember { mutableStateOf(sale.cashierName) }
    var notes by remember { mutableStateOf(sale.notes) }

    var discountInput by remember { mutableStateOf(if (sale.discount > 0) "%.2f".format(sale.discount) else "0") }
    var taxRateInput by remember { mutableStateOf(if (sale.taxRate > 0) "%.1f".format(sale.taxRate) else "0") }
    var paidAmountInput by remember { mutableStateOf("%.2f".format(sale.paidAmount)) }

    // Dynamic Calculations
    val subtotal = remember(items, sale) {
        val calculatedItemsSum = items.sumOf { it.totalPrice }
        if (calculatedItemsSum > 0) calculatedItemsSum else sale.totalAmount
    }

    val discount = discountInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val taxRate = taxRateInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val taxableAmount = (subtotal - discount).coerceAtLeast(0.0)
    val taxAmount = (taxableAmount * taxRate) / 100.0
    val netAmount = (taxableAmount + taxAmount).coerceAtLeast(0.0)

    val paidAmount = paidAmountInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val dueAmount = (netAmount - paidAmount).coerceAtLeast(0.0)
    val changeReturn = (paidAmount - netAmount).coerceAtLeast(0.0)

    var showCustomerPicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("edit_invoice_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = Slate50
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy900)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Gold500
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = Navy900,
                                modifier = Modifier.padding(6.dp).size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Edit Invoice Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${sale.invoiceNumber} • ${dateFormat.format(Date(sale.createdAt))}",
                                fontSize = 11.sp,
                                color = Gold300
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_edit_invoice_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Scrollable Content Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Status Badge Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Invoice Status & Payment Terms",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        StatusBadge(
                            text = if (dueAmount <= 0.0) "PAID IN FULL" else "DUE: $currency %.2f".format(dueAmount),
                            backgroundColor = if (dueAmount <= 0.0) Emerald100 else Rose100,
                            textColor = if (dueAmount <= 0.0) Emerald700 else Rose600
                        )
                    }

                    // 1. Customer Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Customer Information", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy800)
                                if (customersList.isNotEmpty()) {
                                    TextButton(
                                        onClick = { showCustomerPicker = !showCustomerPicker },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (showCustomerPicker) "Hide Customers" else "Select Existing", fontSize = 11.sp)
                                    }
                                }
                            }

                            if (showCustomerPicker && customersList.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Slate100
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Select Registered Customer:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Navy600)
                                        customersList.take(6).forEach { c ->
                                            Surface(
                                                onClick = {
                                                    customerName = c.name
                                                    selectedCustomerId = c.id
                                                    showCustomerPicker = false
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.White,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(c.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Navy900)
                                                    Text("Due: $currency %.0f".format(c.balance), fontSize = 11.sp, color = if (c.balance > 0) Rose600 else Slate500)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("Customer Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Navy700) },
                                modifier = Modifier.fillMaxWidth().testTag("edit_customer_name_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // 2. Payment Method & Cashier
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Payment Mode & Attribution", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy800)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Cash", "Bank / Card", "Credit", "Online").forEach { method ->
                                    FilterChip(
                                        selected = paymentType.equals(method, ignoreCase = true),
                                        onClick = { paymentType = method },
                                        label = { Text(method, fontSize = 11.sp) },
                                        modifier = Modifier.testTag("edit_pay_method_$method")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = cashierName,
                                    onValueChange = { cashierName = it },
                                    label = { Text("Cashier Name") },
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Navy700) },
                                    modifier = Modifier.weight(1f).testTag("edit_cashier_name_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )

                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text("Remarks / Note") },
                                    leadingIcon = { Icon(Icons.Default.Note, contentDescription = null, tint = Navy700) },
                                    modifier = Modifier.weight(1f).testTag("edit_notes_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // 3. Financial Recalculations & Discounts
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Financial Summary & Modifications", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy800)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Items Subtotal (Gross):", fontSize = 13.sp, color = Navy600)
                                Text("$currency %.2f".format(subtotal), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy900)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = discountInput,
                                    onValueChange = { discountInput = it },
                                    label = { Text("Discount ($currency)") },
                                    leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Rose600) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("edit_discount_input")
                                )

                                OutlinedTextField(
                                    value = taxRateInput,
                                    onValueChange = { taxRateInput = it },
                                    label = { Text("Tax Rate (%)") },
                                    leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null, tint = Navy700) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("edit_tax_rate_input")
                                )
                            }

                            HorizontalDivider(color = Slate200)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recalculated Net Total:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                Text("$currency %.2f".format(netAmount), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Emerald700)
                            }

                            HorizontalDivider(color = Slate200)

                            // Paid Amount & Quick Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Amount Collected (Paid):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Navy800)
                                TextButton(
                                    onClick = { paidAmountInput = "%.2f".format(netAmount) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("edit_mark_full_paid_btn")
                                ) {
                                    Text("Mark Full Paid ($currency %.2f)".format(netAmount), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedTextField(
                                value = paidAmountInput,
                                onValueChange = { paidAmountInput = it },
                                label = { Text("Paid Amount ($currency)") },
                                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = Emerald600) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("edit_paid_amount_input")
                            )

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = if (dueAmount > 0) Rose50 else Emerald50
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (dueAmount > 0) "Customer Remaining Due:" else "Change Return:",
                                            fontSize = 11.sp,
                                            color = if (dueAmount > 0) Rose800 else Emerald800,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (dueAmount > 0) "$currency %.2f".format(dueAmount) else "$currency %.2f".format(changeReturn),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (dueAmount > 0) Rose600 else Emerald700
                                        )
                                    }

                                    StatusBadge(
                                        text = if (dueAmount <= 0.0) "CLEARED" else "RECEIVABLE DUE",
                                        backgroundColor = if (dueAmount <= 0.0) Emerald600 else Rose600,
                                        textColor = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // 4. Purchased Items summary preview (Non-destructive)
                    if (items.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Purchased Line Items (${items.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy800)
                                items.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${index + 1}. ${item.productName} × ${item.quantity.toInt()}", fontSize = 12.sp, color = Navy700)
                                        Text("$currency %.2f".format(item.totalPrice), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Navy900)
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Actions
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Delete Button (Danger action)
                        OutlinedButton(
                            onClick = onDeleteRequest,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose600),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("edit_delete_invoice_button")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Invoice", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Invoice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel", color = Navy600)
                        }

                        Button(
                            onClick = {
                                val updatedSale = sale.copy(
                                    customerName = customerName.trim().ifBlank { "Walk-in Customer" },
                                    customerId = selectedCustomerId,
                                    totalAmount = subtotal,
                                    discount = discount,
                                    taxRate = taxRate,
                                    taxAmount = taxAmount,
                                    netAmount = netAmount,
                                    paidAmount = paidAmount,
                                    dueAmount = dueAmount,
                                    paymentType = paymentType,
                                    cashierName = cashierName.trim().ifBlank { "Muhammad Umer" },
                                    notes = notes.trim()
                                )
                                onSave(updatedSale)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("edit_save_invoice_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
