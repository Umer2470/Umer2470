package com.example.ui.calculator

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.StoreCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreCalculatorDialog(
    isUrdu: Boolean = false,
    onDismiss: () -> Unit,
    onApplyToPos: ((Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var expression by remember { mutableStateOf("") }
    var currentNumber by remember { mutableStateOf("0") }
    var lastEvaluatedResult by remember { mutableStateOf(0.0) }
    var showConfirmTransferDialog by remember { mutableStateOf(false) }

    // Quick presets state
    var selectedPreset by remember { mutableStateOf(0) } // 0: None/Standard, 1: Qty*Rate, 2: Discount, 3: Tax, 4: Margin
    var presetInput1 by remember { mutableStateOf("") }
    var presetInput2 by remember { mutableStateOf("") }

    val formattedCurrency = remember(currentNumber, lastEvaluatedResult) {
        val num = currentNumber.toDoubleOrNull() ?: lastEvaluatedResult
        StoreCalculator.formatCurrency(num)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isUrdu) "اسٹور تجارتی کیلکولیٹر" else "Store Commercial Calculator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isUrdu) "آزادانہ حساب کتاب (صرف دکان دار کیلئے)" else "Independent shop utility (Rs)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Display Area
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = expression.ifEmpty { "0" },
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        Text(
                            text = currentNumber,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        Text(
                            text = formattedCurrency,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4ADE80)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar (Copy & Presets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val num = currentNumber.toDoubleOrNull() ?: lastEvaluatedResult
                            clipboardManager.setText(AnnotatedString(StoreCalculator.formatCurrency(num)))
                            Toast.makeText(context, "Copied: ${StoreCalculator.formatCurrency(num)}", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isUrdu) "کاپی کریں" else "Copy", fontSize = 12.sp)
                    }

                    if (onApplyToPos != null) {
                        Button(
                            onClick = { showConfirmTransferDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(if (isUrdu) "پی او ایس میں درج کریں" else "Transfer to POS", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Preset Tool Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedPreset == 1,
                        onClick = { selectedPreset = if (selectedPreset == 1) 0 else 1 },
                        label = { Text("Qty × Rate", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedPreset == 2,
                        onClick = { selectedPreset = if (selectedPreset == 2) 0 else 2 },
                        label = { Text("Discount %", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedPreset == 3,
                        onClick = { selectedPreset = if (selectedPreset == 3) 0 else 3 },
                        label = { Text("Tax %", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedPreset == 4,
                        onClick = { selectedPreset = if (selectedPreset == 4) 0 else 4 },
                        label = { Text("Profit %", fontSize = 11.sp) }
                    )
                }

                // Preset Controls
                if (selectedPreset > 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            when (selectedPreset) {
                                1 -> {
                                    Text("Quantity × Unit Price Calculation", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = presetInput1,
                                            onValueChange = { presetInput1 = it },
                                            label = { Text("Quantity") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = presetInput2,
                                            onValueChange = { presetInput2 = it },
                                            label = { Text("Rate (Rs)") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                val q = presetInput1.toDoubleOrNull() ?: 0.0
                                                val r = presetInput2.toDoubleOrNull() ?: 0.0
                                                val total = StoreCalculator.calculateQuantityPrice(q, r)
                                                expression = "$q × Rs $r ="
                                                currentNumber = total.toString()
                                                lastEvaluatedResult = total
                                            },
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        ) { Text("Calc") }
                                    }
                                }
                                2 -> {
                                    Text("Discount % Calculation", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = presetInput1,
                                            onValueChange = { presetInput1 = it },
                                            label = { Text("Original Price") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = presetInput2,
                                            onValueChange = { presetInput2 = it },
                                            label = { Text("Disc %") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                val p = presetInput1.toDoubleOrNull() ?: 0.0
                                                val d = presetInput2.toDoubleOrNull() ?: 0.0
                                                val res = StoreCalculator.calculateDiscount(p, d)
                                                expression = "Rs $p - ${d}% (Rs ${res.discountAmount}) ="
                                                currentNumber = res.netPayable.toString()
                                                lastEvaluatedResult = res.netPayable
                                            },
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        ) { Text("Calc") }
                                    }
                                }
                                3 -> {
                                    Text("Tax (GST) % Calculation", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = presetInput1,
                                            onValueChange = { presetInput1 = it },
                                            label = { Text("Subtotal") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = presetInput2,
                                            onValueChange = { presetInput2 = it },
                                            label = { Text("Tax %") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                val s = presetInput1.toDoubleOrNull() ?: 0.0
                                                val t = presetInput2.toDoubleOrNull() ?: 17.0
                                                val res = StoreCalculator.calculateTax(s, t)
                                                expression = "Rs $s + ${t}% GST (Rs ${res.taxAmount}) ="
                                                currentNumber = res.grossTotal.toString()
                                                lastEvaluatedResult = res.grossTotal
                                            },
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        ) { Text("Calc") }
                                    }
                                }
                                4 -> {
                                    Text("Profit & Margin % Calculation", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = presetInput1,
                                            onValueChange = { presetInput1 = it },
                                            label = { Text("Sale Price") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = presetInput2,
                                            onValueChange = { presetInput2 = it },
                                            label = { Text("Cost Price") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                val sp = presetInput1.toDoubleOrNull() ?: 0.0
                                                val cp = presetInput2.toDoubleOrNull() ?: 0.0
                                                val res = StoreCalculator.calculateProfit(sp, cp)
                                                expression = "Profit: Rs ${res.grossProfit} (${String.format("%.1f", res.marginPercent)}% Margin)"
                                                currentNumber = res.grossProfit.toString()
                                                lastEvaluatedResult = res.grossProfit
                                            },
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        ) { Text("Calc") }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Standard Keypad Grid
                val buttonRows = listOf(
                    listOf("C", "⌫", "±", "÷"),
                    listOf("7", "8", "9", "×"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("0", ".", "%", "=")
                )

                for (row in buttonRows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (btn in row) {
                            val isOp = btn in listOf("÷", "×", "-", "+", "%")
                            val isAction = btn in listOf("C", "⌫", "±")
                            val isEquals = btn == "="

                            val containerColor = when {
                                isEquals -> MaterialTheme.colorScheme.primary
                                isOp -> MaterialTheme.colorScheme.primaryContainer
                                isAction -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val contentColor = when {
                                isEquals -> MaterialTheme.colorScheme.onPrimary
                                isOp -> MaterialTheme.colorScheme.onPrimaryContainer
                                isAction -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clickable {
                                        when (btn) {
                                            "C" -> {
                                                expression = ""
                                                currentNumber = "0"
                                                lastEvaluatedResult = 0.0
                                            }
                                            "⌫" -> {
                                                currentNumber = if (currentNumber.length > 1) {
                                                    currentNumber.dropLast(1)
                                                } else "0"
                                            }
                                            "±" -> {
                                                currentNumber = if (currentNumber.startsWith("-")) {
                                                    currentNumber.drop(1)
                                                } else if (currentNumber != "0") {
                                                    "-$currentNumber"
                                                } else "0"
                                            }
                                            "+", "-", "×", "÷", "%" -> {
                                                val opSymbol = if (btn == "×") "*" else if (btn == "÷") "/" else btn
                                                expression = "$expression $currentNumber $opSymbol".trim()
                                                currentNumber = "0"
                                            }
                                            "=" -> {
                                                val fullExpr = "$expression $currentNumber".trim()
                                                val res = StoreCalculator.evaluateExpression(fullExpr)
                                                expression = "$fullExpr ="
                                                currentNumber = if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                                                lastEvaluatedResult = res
                                            }
                                            "." -> {
                                                if (!currentNumber.contains(".")) {
                                                    currentNumber += "."
                                                }
                                            }
                                            else -> { // Digits
                                                currentNumber = if (currentNumber == "0") btn else currentNumber + btn
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = containerColor
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = btn,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Explicit Confirmation for Transfer to POS
    if (showConfirmTransferDialog) {
        val transferVal = currentNumber.toDoubleOrNull() ?: lastEvaluatedResult
        AlertDialog(
            onDismissRequest = { showConfirmTransferDialog = false },
            title = { Text(if (isUrdu) "رقم کی تصدیق" else "Confirm Transfer to POS") },
            text = {
                Text(
                    text = if (isUrdu)
                        "کیا آپ واقعی ${StoreCalculator.formatCurrency(transferVal)} پی او ایس میں وصول شدہ رقم کے طور پر درج کرنا چاہتے ہیں؟"
                    else
                        "Do you want to transfer ${StoreCalculator.formatCurrency(transferVal)} into the POS checkout as Amount Received?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmTransferDialog = false
                        onApplyToPos?.invoke(transferVal)
                        onDismiss()
                    }
                ) {
                    Text(if (isUrdu) "ہاں، درج کریں" else "Confirm & Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmTransferDialog = false }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }
}
