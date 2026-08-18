package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Sanitizer
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ShopLogoAvatar
import com.example.ui.viewmodel.StoreViewModel

data class BusinessTypeOption(
    val title: String,
    val icon: ImageVector,
    val description: String
)

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val label: String
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BusinessSetupWizardScreen(
    viewModel: StoreViewModel,
    onSetupCompleted: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }

    // Step Data States
    var businessName by remember { mutableStateOf("") }
    var logoUri by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var whatsappNumber by remember { mutableStateOf("") }
    var sameAsPhone by remember { mutableStateOf(true) }
    var address by remember { mutableStateOf("") }
    var selectedBusinessType by remember { mutableStateOf("Hardware Store") }
    var customBusinessType by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("Rs.") }
    var customCurrencySymbol by remember { mutableStateOf("") }

    // Super Admin States
    var adminFullName by remember { mutableStateOf("") }
    var adminUsername by remember { mutableStateOf("superadmin") }
    var adminPhone by remember { mutableStateOf("") }
    var adminPin by remember { mutableStateOf("1234") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            logoUri = it.toString()
        }
    }

    val businessTypes = listOf(
        BusinessTypeOption("Building Material", Icons.Default.Domain, "Construction, cement, steel, bricks"),
        BusinessTypeOption("Paint Store", Icons.Default.FormatPaint, "Paints, enamel, brushes, wall finish"),
        BusinessTypeOption("Hardware Store", Icons.Default.Hardware, "Tools, fasteners, locks, machinery"),
        BusinessTypeOption("Sanitary Store", Icons.Default.Sanitizer, "Plumbing, tiles, bath fittings"),
        BusinessTypeOption("Grocery Store", Icons.Default.ShoppingCart, "Supermarket, food, general store"),
        BusinessTypeOption("Medical Store", Icons.Default.LocalHospital, "Pharmacy, medicines, healthcare"),
        BusinessTypeOption("Electronics Store", Icons.Default.ElectricalServices, "Appliances, wires, gadgets"),
        BusinessTypeOption("Mobile Shop", Icons.Default.Smartphone, "Cell phones, accessories, repair"),
        BusinessTypeOption("Garments", Icons.Default.Checkroom, "Clothing, apparel, fashion boutique"),
        BusinessTypeOption("Pharmacy", Icons.Default.LocalHospital, "Prescription medicines & care"),
        BusinessTypeOption("Other", Icons.Default.Storefront, "General commercial retail or wholesale")
    )

    val currencies = listOf(
        CurrencyOption("PKR", "Rs.", "Pakistani Rupee (Rs.)"),
        CurrencyOption("USD", "$", "US Dollar ($)"),
        CurrencyOption("EUR", "€", "Euro (€)"),
        CurrencyOption("GBP", "£", "British Pound (£)"),
        CurrencyOption("INR", "₹", "Indian Rupee (₹)"),
        CurrencyOption("SAR", "SR", "Saudi Riyal (SR)"),
        CurrencyOption("AED", "AED", "UAE Dirham (AED)"),
        CurrencyOption("CUSTOM", "Other", "Custom Currency Symbol")
    )

    fun validateAndNext() {
        errorMessage = null
        when (currentStep) {
            1 -> {
                if (businessName.isBlank()) {
                    errorMessage = "Please enter your official Business Name."
                    return
                }
            }
            3 -> {
                if (ownerName.isBlank()) {
                    errorMessage = "Please enter the Business Owner's Name."
                    return
                }
                if (adminFullName.isBlank()) adminFullName = ownerName
            }
            4 -> {
                if (phone.isBlank()) {
                    errorMessage = "Please enter a primary contact phone number."
                    return
                }
                if (sameAsPhone) whatsappNumber = phone
                if (adminPhone.isBlank()) adminPhone = phone
            }
            5 -> {
                if (whatsappNumber.isBlank()) {
                    whatsappNumber = phone
                }
            }
            6 -> {
                if (address.isBlank()) {
                    errorMessage = "Please enter your business physical address."
                    return
                }
            }
            7 -> {
                if (selectedBusinessType == "Other" && customBusinessType.isBlank()) {
                    errorMessage = "Please specify your Business Type."
                    return
                }
            }
            8 -> {
                if (selectedCurrency == "CUSTOM" && customCurrencySymbol.isBlank()) {
                    errorMessage = "Please enter your custom currency symbol."
                    return
                }
            }
            9 -> {
                if (adminFullName.isBlank()) {
                    errorMessage = "Please enter Super Admin Full Name."
                    return
                }
                if (adminUsername.isBlank()) {
                    errorMessage = "Please enter Super Admin Username."
                    return
                }
                if (adminPin.length < 4) {
                    errorMessage = "Security PIN must be at least 4 digits."
                    return
                }

                val finalType = if (selectedBusinessType == "Other") customBusinessType else selectedBusinessType
                val finalCurrency = if (selectedCurrency == "CUSTOM") customCurrencySymbol else selectedCurrency
                val finalWhatsApp = if (sameAsPhone || whatsappNumber.isBlank()) phone else whatsappNumber

                // Save onboarding config
                viewModel.completeBusinessOnboarding(
                    businessName = businessName.trim(),
                    ownerName = ownerName.trim(),
                    phone = phone.trim(),
                    whatsappNumber = finalWhatsApp.trim(),
                    address = address.trim(),
                    businessType = finalType.trim(),
                    currencySymbol = finalCurrency.trim(),
                    logoUri = logoUri,
                    adminName = adminFullName.trim(),
                    adminUsername = adminUsername.trim(),
                    adminPin = adminPin.trim(),
                    adminPhone = adminPhone.trim()
                )

                onSetupCompleted()
                return
            }
        }

        if (currentStep < 9) {
            currentStep++
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F2537)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 1) {
                    IconButton(onClick = { currentStep-- }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Text(
                    text = "Business Setup Wizard",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )

                Text(
                    text = "Step $currentStep of 9",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Animated Progress Indicator
            LinearProgressIndicator(
                progress = { currentStep / 9f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Color(0xFFFFD700),
                trackColor = Color.White.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Card Container for Steps
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        AnimatedContent(
                            targetState = currentStep,
                            transitionSpec = { fadeIn() with fadeOut() },
                            label = "WizardStepAnimation"
                        ) { step ->
                            when (step) {
                                1 -> Step1BusinessName(
                                    businessName = businessName,
                                    onValueChange = { businessName = it }
                                )
                                2 -> Step2BusinessLogo(
                                    logoUri = logoUri,
                                    onSelectLogo = { logoUri = it },
                                    onPickImage = { imagePickerLauncher.launch("image/*") }
                                )
                                3 -> Step3OwnerName(
                                    ownerName = ownerName,
                                    onValueChange = { ownerName = it }
                                )
                                4 -> Step4PhoneNumber(
                                    phone = phone,
                                    onValueChange = { phone = it }
                                )
                                5 -> Step5WhatsAppNumber(
                                    phone = phone,
                                    whatsappNumber = whatsappNumber,
                                    sameAsPhone = sameAsPhone,
                                    onSameAsPhoneChange = {
                                        sameAsPhone = it
                                        if (it) whatsappNumber = phone
                                    },
                                    onWhatsAppChange = { whatsappNumber = it }
                                )
                                6 -> Step6BusinessAddress(
                                    address = address,
                                    onValueChange = { address = it }
                                )
                                7 -> Step7BusinessType(
                                    selectedType = selectedBusinessType,
                                    customType = customBusinessType,
                                    types = businessTypes,
                                    onSelectType = { selectedBusinessType = it },
                                    onCustomTypeChange = { customBusinessType = it }
                                )
                                8 -> Step8Currency(
                                    selectedCurrency = selectedCurrency,
                                    customSymbol = customCurrencySymbol,
                                    currencies = currencies,
                                    onSelectCurrency = { selectedCurrency = it },
                                    onCustomSymbolChange = { customCurrencySymbol = it }
                                )
                                9 -> Step9CreateSuperAdmin(
                                    fullName = adminFullName,
                                    username = adminUsername,
                                    pin = adminPin,
                                    phone = adminPhone,
                                    onFullNameChange = { adminFullName = it },
                                    onUsernameChange = { adminUsername = it },
                                    onPinChange = { adminPin = it },
                                    onPhoneChange = { adminPhone = it }
                                )
                            }
                        }

                        errorMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                            ) {
                                Text(
                                    text = msg,
                                    color = Color(0xFF991B1B),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Next / Complete Button
                    Button(
                        onClick = { validateAndNext() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStep == 9) Color(0xFF059669) else Color(0xFF0F2537)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (currentStep == 9) Icons.Default.RocketLaunch else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentStep == 9) "Complete Setup & Launch POS" else "Next Step",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepHeader(
    stepNumber: Int,
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Step $stepNumber: $title",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun Step1BusinessName(
    businessName: String,
    onValueChange: (String) -> Unit
) {
    Column {
        StepHeader(
            stepNumber = 1,
            title = "Business Name",
            subtitle = "Enter your official company, store, or shop commercial name",
            icon = Icons.Default.Store
        )

        OutlinedTextField(
            value = businessName,
            onValueChange = onValueChange,
            label = { Text("Store / Business Name *") },
            placeholder = { Text("e.g. Metro Building Material & Hardware") },
            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFF2563EB)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun Step2BusinessLogo(
    logoUri: String,
    onSelectLogo: (String) -> Unit,
    onPickImage: () -> Unit
) {
    Column {
        StepHeader(
            stepNumber = 2,
            title = "Business Logo",
            subtitle = "Upload your custom brand logo or choose a commercial preset avatar",
            icon = Icons.Default.PhotoCamera
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShopLogoAvatar(logoUri = logoUri, size = 100.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPickImage,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Image from Device")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Or pick a store icon style:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val presets = listOf("", "PRESET_BUILDING", "PRESET_PAINT", "PRESET_HARDWARE", "PRESET_GROCERY")
                presets.forEach { preset ->
                    Surface(
                        onClick = { onSelectLogo(preset) },
                        shape = CircleShape,
                        color = if (logoUri == preset) Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                        border = if (logoUri == preset) BorderStroke(2.dp, Color(0xFF2563EB)) else null,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (preset) {
                                    "PRESET_BUILDING" -> Icons.Default.Domain
                                    "PRESET_PAINT" -> Icons.Default.FormatPaint
                                    "PRESET_HARDWARE" -> Icons.Default.Hardware
                                    "PRESET_GROCERY" -> Icons.Default.ShoppingCart
                                    else -> Icons.Default.Storefront
                                },
                                contentDescription = null,
                                tint = Color(0xFF1E40AF)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Step3OwnerName(
    ownerName: String,
    onValueChange: (String) -> Unit
) {
    Column {
        StepHeader(
            stepNumber = 3,
            title = "Owner Name",
            subtitle = "Enter the name of the store proprietor, founder, or director",
            icon = Icons.Default.Person
        )

        OutlinedTextField(
            value = ownerName,
            onValueChange = onValueChange,
            label = { Text("Owner / Proprietor Full Name *") },
            placeholder = { Text("e.g. Alexander Vance") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2563EB)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun Step4PhoneNumber(
    phone: String,
    onValueChange: (String) -> Unit
) {
    Column {
        StepHeader(
            stepNumber = 4,
            title = "Phone Number",
            subtitle = "Primary customer contact phone for orders & support",
            icon = Icons.Default.Phone
        )

        OutlinedTextField(
            value = phone,
            onValueChange = onValueChange,
            label = { Text("Primary Phone Number *") },
            placeholder = { Text("e.g. +1 555 019 2831 or 0300 1234567") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2563EB)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun Step5WhatsAppNumber(
    phone: String,
    whatsappNumber: String,
    sameAsPhone: Boolean,
    onSameAsPhoneChange: (Boolean) -> Unit,
    onWhatsAppChange: (String) -> Unit
) {
    Column {
        StepHeader(
            stepNumber = 5,
            title = "WhatsApp Number",
            subtitle = "Used to send instant digital invoices & receipts directly to customers via WhatsApp",
            icon = Icons.Default.Phone
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onSameAsPhoneChange(!sameAsPhone) }
        ) {
            Checkbox(
                checked = sameAsPhone,
                onCheckedChange = onSameAsPhoneChange
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Same as primary Phone Number ($phone)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!sameAsPhone) {
            OutlinedTextField(
                value = whatsappNumber,
                onValueChange = onWhatsAppChange,
                label = { Text("WhatsApp Business Number *") },
                placeholder = { Text("e.g. +1 555 019 2831") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF059669)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun Step6BusinessAddress(
    address: String,
    onValueChange: (String) -> Unit
) {
    Column {
        StepHeader(
            stepNumber = 6,
            title = "Business Address",
            subtitle = "Store location printed on official printed & PDF receipts",
            icon = Icons.Default.LocationOn
        )

        OutlinedTextField(
            value = address,
            onValueChange = onValueChange,
            label = { Text("Store Address *") },
            placeholder = { Text("e.g. Main Commercial Plaza, Market Road") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF2563EB)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun Step7BusinessType(
    selectedType: String,
    customType: String,
    types: List<BusinessTypeOption>,
    onSelectType: (String) -> Unit,
    onCustomTypeChange: (String) -> Unit
) {
    Column {
        StepHeader(
            stepNumber = 7,
            title = "Business Type",
            subtitle = "Select your store industry sector to optimize default categories & settings",
            icon = Icons.Default.Apartment
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(280.dp)
        ) {
            items(types) { item ->
                val isSelected = selectedType == item.title
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectType(item.title) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF1E40AF) else Color(0xFF334155)
                        )
                    }
                }
            }
        }

        if (selectedType == "Other") {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = customType,
                onValueChange = onCustomTypeChange,
                label = { Text("Custom Business Type *") },
                placeholder = { Text("e.g. Auto Parts & Hardware") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun Step8Currency(
    selectedCurrency: String,
    customSymbol: String,
    currencies: List<CurrencyOption>,
    onSelectCurrency: (String) -> Unit,
    onCustomSymbolChange: (String) -> Unit
) {
    Column {
        StepHeader(
            stepNumber = 8,
            title = "Currency",
            subtitle = "Choose standard currency symbol for POS transactions & ledgers",
            icon = Icons.Default.AccountBalanceWallet
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            currencies.forEach { item ->
                val isSelected = selectedCurrency == item.symbol || (selectedCurrency == "CUSTOM" && item.code == "CUSTOM")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCurrency(item.symbol) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.symbol,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2563EB))
                        }
                    }
                }
            }
        }

        if (selectedCurrency == "CUSTOM" || selectedCurrency == "Other") {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = customSymbol,
                onValueChange = onCustomSymbolChange,
                label = { Text("Custom Currency Symbol *") },
                placeholder = { Text("e.g. KSh, ৳, Fr") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun Step9CreateSuperAdmin(
    fullName: String,
    username: String,
    pin: String,
    phone: String,
    onFullNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit
) {
    Column {
        StepHeader(
            stepNumber = 9,
            title = "Create Super Admin",
            subtitle = "Configure master administrator credentials for complete store management",
            icon = Icons.Default.AdminPanelSettings
        )

        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = { Text("Super Admin Full Name *") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2563EB)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Admin Login Username *") },
            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF2563EB)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = onPinChange,
            label = { Text("Security PIN / Password (4-6 digits) *") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2563EB)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Admin Contact Phone *") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2563EB)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
