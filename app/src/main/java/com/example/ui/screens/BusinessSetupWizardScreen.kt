package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    var storeName by remember(storeSettings) { mutableStateOf(storeSettings?.storeName ?: "CH UMER POS.03080018035") }
    var ownerName by remember(storeSettings) { mutableStateOf(storeSettings?.ownerName ?: "CH UMER") }
    var phone by remember(storeSettings) { mutableStateOf(storeSettings?.phone ?: "03080018035") }
    var address by remember(storeSettings) { mutableStateOf(storeSettings?.address ?: "Main Market, Store #1") }
    var currency by remember(storeSettings) { mutableStateOf(storeSettings?.currencySymbol ?: "Rs") }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Business Profile Setup",
                subtitle = "Store Details & Invoice Branding",
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(title = "Store Profile", subtitle = "Appears on receipts and invoices")

            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("Business / Store Name") },
                modifier = Modifier.fillMaxWidth().testTag("setup_store_name")
            )

            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("Owner / Contact Person") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Support Phone (03080018035)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Store Address") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it },
                label = { Text("Currency Symbol (e.g. Rs, $, AED)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val updated = (storeSettings ?: StoreSettings()).copy(
                        storeName = storeName,
                        ownerName = ownerName,
                        phone = phone,
                        address = address,
                        currencySymbol = currency
                    )
                    viewModel.updateStoreSettings(updated)
                    onNavigateBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                modifier = Modifier.fillMaxWidth().testTag("save_setup_button")
            ) {
                Text("Save Profile Changes")
            }
        }
    }
}
