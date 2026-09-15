package com.example.ui.invoice

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class ReturnItemSelection(
    val saleItem: SaleItem,
    var isSelected: Boolean = false,
    var returnQuantity: Double = 1.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnInvoiceDialog(
    sale: Sale,
    items: List<SaleItem>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirmReturn: (
        returnedItems: List<Pair<SaleItem, Double>>, // item to quantity
        refundPaymentType: String,
        refundAmount: Double,
        reason: String
    ) -> Unit
) {
    val context = LocalContext.current
    var reason by remember { mutableStateOf("") }
    var refundPaymentType by remember { mutableStateOf(if (sale.paymentType.equals("Credit", true)) "Credit" else "Cash") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Track selections for each item
    val selections = remember(items) {
        items.map { item ->
            mutableStateOf(
                ReturnItemSelection(
                    saleItem = item,
                    isSelected = false,
                    returnQuantity = item.quantity
                )
            )
        }
    }

    val selectedItemsWithQty by remember {
        derivedStateOf {
            selections.filter { it.value.isSelected && it.value.returnQuantity > 0 }
                .map { Pair(it.value.saleItem, it.value.returnQuantity) }
        }
    }

    val calculatedRefundAmount by remember {
        derivedStateOf {
            selectedItemsWithQty.sumOf { (item, qty) ->
                item.salePrice * qty
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sales Return / Exchange",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = "Original Invoice #${sale.invoiceNumber} • ${sale.customerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Navy500
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Navy700)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Info banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Blue50
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Navy700, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Original invoice will be preserved. Returned items will be restored to inventory.",
                            fontSize = 11.sp,
                            color = Navy900
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select Items to Return:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Navy900
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Items list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selections) { itemState ->
                        val sel = itemState.value
                        val item = sel.saleItem

                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (sel.isSelected) Amber50 else Slate50
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (sel.isSelected) Amber500 else Slate200
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = sel.isSelected,
                                            onCheckedChange = { checked ->
                                                itemState.value = sel.copy(isSelected = checked)
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = Amber500)
                                        )
                                        Column {
                                            Text(
                                                text = item.productName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Navy900
                                            )
                                            Text(
                                                text = "Sold: ${item.quantity} ${item.unit} @ $currency ${"%.2f".format(item.salePrice)}",
                                                fontSize = 11.sp,
                                                color = Navy500
                                            )
                                        }
                                    }

                                    if (sel.isSelected) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (sel.returnQuantity > 1.0) {
                                                        itemState.value = sel.copy(returnQuantity = sel.returnQuantity - 1.0)
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Navy700)
                                            }

                                            OutlinedTextField(
                                                value = if (sel.returnQuantity == sel.returnQuantity.toLong().toDouble()) sel.returnQuantity.toLong().toString() else sel.returnQuantity.toString(),
                                                onValueChange = { str ->
                                                    val q = str.toDoubleOrNull() ?: 0.0
                                                    itemState.value = sel.copy(returnQuantity = q.coerceIn(0.0, item.quantity))
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.width(60.dp),
                                                singleLine = true,
                                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            )

                                            IconButton(
                                                onClick = {
                                                    if (sel.returnQuantity < item.quantity) {
                                                        itemState.value = sel.copy(returnQuantity = sel.returnQuantity + 1.0)
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = Navy700)
                                            }
                                        }
                                    }
                                }

                                if (sel.isSelected) {
                                    val lineRefund = item.salePrice * sel.returnQuantity
                                    Text(
                                        text = "Line Refund: $currency ${"%.2f".format(lineRefund)}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = Amber700,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Refund details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Refund Amount:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                            Text(
                                "$currency %.2f".format(calculatedRefundAmount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Rose600
                            )
                        }

                        Text("Refund Disbursed Through:", fontSize = 11.sp, color = Navy700)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Cash", "Credit", "Bank").forEach { mode ->
                                FilterChip(
                                    selected = refundPaymentType == mode,
                                    onClick = { refundPaymentType = mode },
                                    label = {
                                        Text(
                                            when (mode) {
                                                "Cash" -> "Cash (Drawer Out)"
                                                "Credit" -> "Customer Due Adjustment"
                                                else -> "Bank Transfer"
                                            },
                                            fontSize = 10.sp
                                        )
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("Reason for Return / Exchange") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (isSubmitting) return@Button
                            if (selectedItemsWithQty.isEmpty()) {
                                Toast.makeText(context, "Please select at least one item to return.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (reason.isBlank()) {
                                Toast.makeText(context, "Please enter a reason for the return.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSubmitting = true
                            onConfirmReturn(
                                selectedItemsWithQty,
                                refundPaymentType,
                                calculatedRefundAmount,
                                reason
                            )
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = Amber500),
                        modifier = Modifier.weight(1f).testTag("confirm_return_button")
                    ) {
                        Icon(Icons.Default.KeyboardReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSubmitting) "Processing..." else "Process Return", color = Navy900, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
