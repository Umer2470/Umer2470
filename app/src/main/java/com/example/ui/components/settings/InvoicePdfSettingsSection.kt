package com.example.ui.components.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.StoreSettings
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.util.PdfGenerator
import com.example.util.PosSettingsManager

@Composable
fun InvoicePdfSettingsSection(
    settingsManager: PosSettingsManager,
    storeSettings: StoreSettings?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var tagline by remember { mutableStateOf(settingsManager.getPdfTagline()) }
    var selectedAccent by remember { mutableStateOf(settingsManager.getPdfAccentColor()) }
    var footerText by remember { mutableStateOf(settingsManager.getPdfFooterText()) }
    var showContact by remember { mutableStateOf(settingsManager.isPdfShowContactInfo()) }
    var showAddress by remember { mutableStateOf(settingsManager.isPdfShowAddress()) }
    var showTaxReg by remember { mutableStateOf(settingsManager.isPdfShowTaxReg()) }
    var taxRegNumber by remember { mutableStateOf(settingsManager.getPdfTaxRegNumber()) }

    var showSaveToast by remember { mutableStateOf(false) }

    val accentPalette = listOf(
        Pair("Navy", Color(0xFF0F172A)),
        Pair("Royal Blue", Color(0xFF1D4ED8)),
        Pair("Emerald", Color(0xFF047857)),
        Pair("Deep Purple", Color(0xFF6D28D9)),
        Pair("Crimson Red", Color(0xFFBE123C)),
        Pair("Teal", Color(0xFF0F766E))
    )

    val currentAccentColor = accentPalette.find { it.first == selectedAccent }?.second ?: Color(0xFF0F172A)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(
            title = "Invoice PDF & Document Styling",
            subtitle = "A4 Tax invoice customization, accent themes, taglines & tax IDs"
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("pdf_settings_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. PDF Tagline
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text("PDF Invoice Tagline") },
                    placeholder = { Text("e.g. Professional Retail & Commercial Management") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("pdf_tagline_input")
                )

                // 2. Accent Color Selection
                Text("Invoice PDF Accent Color Theme", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    accentPalette.forEach { (name, color) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(color, CircleShape)
                                    .clickable { selectedAccent = name }
                                    .border(
                                        width = if (selectedAccent == name) 3.dp else 1.dp,
                                        color = if (selectedAccent == name) Gold500 else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .testTag("pdf_accent_$name"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedAccent == name) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Text(
                                text = name.split(" ").first(),
                                fontSize = 10.sp,
                                fontWeight = if (selectedAccent == name) FontWeight.Bold else FontWeight.Normal,
                                color = Navy900
                            )
                        }
                    }
                }

                Divider(color = Slate200)

                // 3. Footer & Legal Notice
                OutlinedTextField(
                    value = footerText,
                    onValueChange = { footerText = it },
                    label = { Text("PDF Invoice Footer & Warranty Notice") },
                    placeholder = { Text("Thank you for your business! Retain this invoice for warranty.") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("pdf_footer_input")
                )

                // 4. Tax / Business Registration (NTN/STRN)
                OutlinedTextField(
                    value = taxRegNumber,
                    onValueChange = { taxRegNumber = it },
                    label = { Text("Tax Registration / NTN / STRN") },
                    placeholder = { Text("e.g. NTN: 9028471-3 • STRN: 3277876123456") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("pdf_tax_reg_input")
                )

                Divider(color = Slate200)

                // 5. Visibility Toggles
                Text("Document Information Display", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)

                PdfToggleRow(
                    title = "Include Store Contact & Phone",
                    subtitle = "Prints official contact number and support email on top header",
                    checked = showContact,
                    onCheckedChange = { showContact = it },
                    testTag = "toggle_pdf_contact"
                )

                PdfToggleRow(
                    title = "Include Physical Store Address",
                    subtitle = "Shows store branch location on header bar",
                    checked = showAddress,
                    onCheckedChange = { showAddress = it },
                    testTag = "toggle_pdf_address"
                )

                PdfToggleRow(
                    title = "Print Tax / Registration Number",
                    subtitle = "Shows official NTN/STRN or commercial tax badge",
                    checked = showTaxReg,
                    onCheckedChange = { showTaxReg = it },
                    testTag = "toggle_pdf_tax_reg"
                )

                // 6. Action Button: Save
                Button(
                    onClick = {
                        settingsManager.setPdfTagline(tagline)
                        settingsManager.setPdfAccentColor(selectedAccent)
                        settingsManager.setPdfFooterText(footerText)
                        settingsManager.setPdfShowContactInfo(showContact)
                        settingsManager.setPdfShowAddress(showAddress)
                        settingsManager.setPdfShowTaxReg(showTaxReg)
                        settingsManager.setPdfTaxRegNumber(taxRegNumber)
                        showSaveToast = true
                        Toast.makeText(context, "Invoice PDF settings saved successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.fillMaxWidth().testTag("btn_save_pdf_settings")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save PDF Document Settings", fontWeight = FontWeight.Bold)
                }

                if (showSaveToast) {
                    Surface(
                        color = Emerald50,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(16.dp))
                            Text("PDF styling configurations updated and applied to future invoices.", color = Emerald800, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 7. REAL-TIME A4 INVOICE PREVIEW CARD
        // ==========================================
        Text("A4 Tax Invoice Layout Preview", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)

        Card(
            modifier = Modifier.fillMaxWidth().testTag("pdf_preview_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate100),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate300)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // A4 Document Sheet Representation
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth().padding(4.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top Colored Header Accent Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(currentAccentColor)
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = storeSettings?.appDisplayName ?: "CHOUDHRY POS APP",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = currentAccentColor
                                    )
                                    Text(
                                        text = tagline.ifBlank { "Professional Retail & Business Management" },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate600
                                    )
                                    if (showAddress) {
                                        Text(
                                            text = storeSettings?.address ?: "Main Market, Store #1",
                                            fontSize = 9.5.sp,
                                            color = Slate600
                                        )
                                    }
                                    if (showContact) {
                                        Text(
                                            text = "Phone: ${storeSettings?.phone ?: "03080018035"} • Email: ${storeSettings?.email ?: "sentrystore.pk@gmail.com"}",
                                            fontSize = 9.sp,
                                            color = Slate600
                                        )
                                    }
                                }

                                Surface(
                                    color = currentAccentColor,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text("TAX INVOICE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("#INV-2026-0042", fontSize = 9.sp, color = Slate200)
                                    }
                                }
                            }

                            if (showTaxReg && taxRegNumber.isNotBlank()) {
                                Surface(
                                    color = Slate100,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = taxRegNumber,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Navy900,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Table Header
                            Surface(
                                color = currentAccentColor,
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Description", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(2f))
                                    Text("Qty", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                                    Text("Rate", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    Text("Amount", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                }
                            }

                            // Sample Rows
                            PdfSampleRow("Mineral Water 1.5L", "2.0", "80.00", "160.00")
                            PdfSampleRow("Cooking Oil 1L Premium Can", "1.0", "480.00", "480.00")

                            Divider(color = Slate200)

                            // Totals
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.width(180.dp)
                                ) {
                                    PdfTotalRow("Subtotal:", "640.00")
                                    PdfTotalRow("Discount:", "-20.00")
                                    PdfTotalRow("GST / Tax:", "31.00")
                                    Divider(color = Slate300, modifier = Modifier.padding(vertical = 2.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total (Rs):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = currentAccentColor)
                                        Text("651.00", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = currentAccentColor)
                                    }
                                }
                            }

                            // Footer Note
                            Divider(color = Slate200)
                            Text(
                                text = footerText.ifBlank { "Thank you for shopping with us! Please retain this invoice." },
                                fontSize = 8.5.sp,
                                color = Slate600,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "© 2027 CHOUDHURY POS. All Rights Reserved.",
                                fontSize = 7.5.sp,
                                color = Slate400,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Navy900)
            Text(subtitle, fontSize = 11.sp, color = Slate600)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun PdfSampleRow(name: String, qty: String, rate: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, fontSize = 9.sp, color = Navy900, modifier = Modifier.weight(2f))
        Text(qty, fontSize = 9.sp, color = Slate700, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
        Text(rate, fontSize = 9.sp, color = Slate700, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        Text(amount, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Navy900, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
    }
}

@Composable
private fun PdfTotalRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 9.5.sp, color = Slate700)
        Text(value, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
    }
}
