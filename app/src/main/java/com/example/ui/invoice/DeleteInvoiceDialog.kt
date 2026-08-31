package com.example.ui.invoice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Sale
import com.example.ui.theme.*

@Composable
fun DeleteInvoiceDialog(
    sale: Sale,
    currency: String,
    onDismiss: () -> Unit,
    onConfirmDelete: (restoreStock: Boolean) -> Unit
) {
    var restoreStock by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Rose100
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Rose600,
                    modifier = Modifier.padding(10.dp).size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Delete Invoice ${sale.invoiceNumber}?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Navy900
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "This will move Invoice #${sale.invoiceNumber} ($currency %.2f for ${sale.customerName}) to the Recycle Bin.".format(sale.netAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Navy700
                )

                if (sale.dueAmount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Amber50,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Note: Customer has pending due of $currency %.2f. Deleting this invoice will reverse and deduct this amount from the customer's balance.".format(sale.dueAmount),
                            style = MaterialTheme.typography.bodySmall,
                            color = Amber800,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = restoreStock,
                            onCheckedChange = { restoreStock = it },
                            colors = CheckboxDefaults.colors(checkedColor = Navy900),
                            modifier = Modifier.testTag("restore_stock_checkbox")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Restore Product Inventory Quantities",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            Text(
                                text = "Return sold items back to store stock automatically",
                                fontSize = 11.sp,
                                color = Navy600
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDelete(restoreStock) },
                colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_delete_invoice_button")
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete to Trash", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Navy600)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}
