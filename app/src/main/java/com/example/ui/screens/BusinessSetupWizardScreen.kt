package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BusinessProfile
import com.example.data.entity.StoreSettings
import com.example.ui.components.AppHeader
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

@Composable
fun BusinessSetupWizardScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val storeSettings by viewModel.storeSettings.collectAsState()
    val businessProfile by viewModel.businessProfile.collectAsState()

    var businessName by remember(storeSettings, businessProfile) {
        mutableStateOf(businessProfile?.businessName ?: storeSettings?.storeName ?: "CH UMER POS.03080018035")
    }
    var tagline by remember(businessProfile) {
        mutableStateOf(businessProfile?.tagline ?: "Smart Retail & Sanitary Wholesale POS")
    }
    var ownerName by remember(storeSettings) {
        mutableStateOf(storeSettings?.ownerName ?: "CH UMER")
    }
    var supportPhone by remember(storeSettings, businessProfile) {
        mutableStateOf(businessProfile?.supportPhone ?: storeSettings?.phone ?: "03080018035")
    }
    var supportEmail by remember(storeSettings, businessProfile) {
        mutableStateOf(businessProfile?.supportEmail ?: storeSettings?.email ?: "sentrystore.pk@gmail.com")
    }
    var website by remember(businessProfile) {
        mutableStateOf(businessProfile?.website ?: "https://sentrystore.pk")
    }
    var address by remember(storeSettings) {
        mutableStateOf(storeSettings?.address ?: "Main Market, Store #1")
    }
    var taxNumber by remember(businessProfile) {
        mutableStateOf(businessProfile?.taxNumber ?: "")
    }
    var registrationNumber by remember(businessProfile) {
        mutableStateOf(businessProfile?.registrationNumber ?: "")
    }
    var currency by remember(storeSettings) {
        mutableStateOf(storeSettings?.currencySymbol ?: "Rs")
    }
    var footerText by remember(storeSettings) {
        mutableStateOf(storeSettings?.invoiceFooterText ?: "Thank you for shopping with us! No return without receipt.")
    }

    var showSuccessToast by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Business Profile & Store Info",
                subtitle = "Manage brand identity, contact & tax credentials",
                onBackClick = onNavigateBack
            )
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = Gold500,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = Navy900,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            text = businessName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = tagline,
                            fontSize = 12.sp,
                            color = Gold400
                        )
                    }
                }
            }

            // 1. BUSINESS IDENTITY
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader(
                        title = "Business Identity",
                        subtitle = "Store name and owner information"
                    )

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business / Store Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_store_name"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tagline,
                        onValueChange = { tagline = it },
                        label = { Text("Business Type / Tagline") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Owner / Manager Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // 2. CONTACT INFORMATION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader(
                        title = "Contact Information",
                        subtitle = "Support phone, WhatsApp, email and website"
                    )

                    OutlinedTextField(
                        value = supportPhone,
                        onValueChange = { supportPhone = it },
                        label = { Text("Phone / WhatsApp (03080018035)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_phone"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = supportEmail,
                        onValueChange = { supportEmail = it },
                        label = { Text("Support Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = { Text("Website / Portal URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // 3. LOCATION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader(
                        title = "Store Location",
                        subtitle = "Physical address on receipts and invoices"
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Physical Store Address") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_address"),
                        maxLines = 2
                    )
                }
            }

            // 4. LEGAL / TAX / RECEIPT SETTINGS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader(
                        title = "Tax & Invoice Settings",
                        subtitle = "NTN, GST, currency & receipt footer"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = taxNumber,
                            onValueChange = { taxNumber = it },
                            label = { Text("NTN / Tax Number") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = registrationNumber,
                            onValueChange = { registrationNumber = it },
                            label = { Text("GST / Reg #") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("Currency Symbol (e.g. Rs, $, AED)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = footerText,
                        onValueChange = { footerText = it },
                        label = { Text("Invoice / Thermal Receipt Footer Message") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    val updatedSettings = (storeSettings ?: StoreSettings()).copy(
                        storeName = businessName,
                        ownerName = ownerName,
                        phone = supportPhone,
                        email = supportEmail,
                        address = address,
                        currencySymbol = currency,
                        invoiceFooterText = footerText
                    )
                    val updatedProfile = (businessProfile ?: BusinessProfile()).copy(
                        businessName = businessName,
                        tagline = tagline,
                        supportPhone = supportPhone,
                        supportEmail = supportEmail,
                        website = website,
                        taxNumber = taxNumber,
                        registrationNumber = registrationNumber,
                        isSetupCompleted = true
                    )
                    viewModel.updateStoreSettings(updatedSettings)
                    viewModel.updateBusinessProfile(updatedProfile)
                    showSuccessToast = true
                    onNavigateBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_setup_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Save & Apply Profile",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
