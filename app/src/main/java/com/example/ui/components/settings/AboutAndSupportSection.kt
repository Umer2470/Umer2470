package com.example.ui.components.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.util.FeatureRequestItem
import com.example.util.PosSettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAndSupportSection(
    settingsManager: PosSettingsManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showAboutDialog by remember { mutableStateOf(false) }
    var showTosDialog by remember { mutableStateOf(false) }
    var showFeatureRequestDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var configDialogType by remember { mutableStateOf("") } // "EMAIL", "WHATSAPP", "PORTAL", "IOS", "WINDOWS"

    // Dialog Input State for configuration
    var configInputValue by remember { mutableStateOf("") }

    // Feature Request State
    var featTitle by remember { mutableStateOf("") }
    var featDesc by remember { mutableStateOf("") }
    var featContact by remember { mutableStateOf("") }
    var featError by remember { mutableStateOf<String?>(null) }

    fun safeOpenUrl(urlStr: String, fallbackPrompt: String, typeKey: String) {
        val cleanUrl = urlStr.trim()
        if (cleanUrl.isBlank() || (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://"))) {
            configDialogType = typeKey
            configInputValue = cleanUrl
            showConfigDialog = true
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open link: ${e.localizedMessage ?: "No browser app found"}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openEmailSupport() {
        val email = settingsManager.getSupportEmail().trim()
        if (email.isBlank()) {
            configDialogType = "EMAIL"
            configInputValue = ""
            showConfigDialog = true
            return
        }
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, "Chaudhry POS App - Support Inquiry")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No email client found. Support Email: $email", Toast.LENGTH_LONG).show()
        }
    }

    fun openWhatsappSupport() {
        val number = settingsManager.getSupportWhatsapp().trim()
        if (number.isBlank()) {
            configDialogType = "WHATSAPP"
            configInputValue = ""
            showConfigDialog = true
            return
        }
        val cleanNumber = number.replace("+", "").replace("-", "").replace(" ", "")
        val waUrl = "https://wa.me/$cleanNumber"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp not installed. Contact: $number", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PLATFORM & DOWNLOADS / پلیٹ فارم اور ڈاؤن لوڈز
        SectionHeader(
            title = "Platform & Downloads / پلیٹ فارم اور ڈاؤن لوڈز",
            subtitle = "Cloud Web Portal, Windows Desktop package, Android APK & connection status"
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("platform_downloads_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection Status & Version
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Emerald50, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Emerald600, CircleShape)
                        )
                        Column {
                            Text(
                                text = "Central Cloud Backend Active",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Emerald900
                            )
                            Text(
                                text = "Engine v8.0.0 • Multi-Shop Isolated",
                                fontSize = 11.sp,
                                color = Emerald700
                            )
                        }
                    }
                    Text(
                        text = "Android v1.0",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = Navy700
                    )
                }

                // 1. Open Web Portal
                Button(
                    onClick = {
                        val portalUrl = settingsManager.getPortalUrl()
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(portalUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_open_web_portal"),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = Gold400, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Live Web POS Portal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // 2. Download Android APK & Windows Desktop App in a Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val apkUrl = "${settingsManager.getPortalUrl()}/downloads/choudhury-pos-app.apk"
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open download: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("btn_download_apk"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = Emerald600, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download APK", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val winUrl = settingsManager.getWindowsDownloadUrl()
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(winUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open download: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("btn_download_windows"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = Blue600, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Windows App", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Installation Instructions info box
                Surface(
                    color = Slate50,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "💡 Help & Installation Instructions:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Navy900
                        )
                        Text(
                            text = "• Android: Download the 50 MB APK and tap install. Enable 'Install unknown apps' if prompted.\n• Windows: Download and extract the 112 MB ZIP package, then run 'Install-ChoudhuryPOS.bat' to create your Desktop & Start Menu shortcuts.",
                            fontSize = 10.5.sp,
                            color = Slate700,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Section Header
        SectionHeader(
            title = "About & Official Support",
            subtitle = "App information, developer details, support channels & portal"
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("about_support_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. About
                SupportMenuItem(
                    icon = Icons.Default.Info,
                    title = "About Chaudhry POS",
                    subtitle = "Version 2.4.0, build details & software specifications",
                    onClick = { showAboutDialog = true },
                    testTag = "menu_about_app"
                )

                Divider(color = Slate200, modifier = Modifier.padding(vertical = 4.dp))

                // 2. Terms of Service
                SupportMenuItem(
                    icon = Icons.Default.Description,
                    title = "Terms of Service",
                    subtitle = "Commercial software license agreement & privacy",
                    onClick = { showTosDialog = true },
                    testTag = "menu_terms_of_service"
                )

                Divider(color = Slate200, modifier = Modifier.padding(vertical = 4.dp))

                // 3. Email Support
                SupportMenuItem(
                    icon = Icons.Default.Email,
                    title = "Email Support",
                    subtitle = settingsManager.getSupportEmail(),
                    onClick = { openEmailSupport() },
                    onEditClick = {
                        configDialogType = "EMAIL"
                        configInputValue = settingsManager.getSupportEmail()
                        showConfigDialog = true
                    },
                    testTag = "menu_email_support"
                )

                Divider(color = Slate200, modifier = Modifier.padding(vertical = 4.dp))

                // 4. WhatsApp Support
                SupportMenuItem(
                    icon = Icons.Default.Chat,
                    title = "WhatsApp Support",
                    subtitle = settingsManager.getSupportWhatsapp(),
                    onClick = { openWhatsappSupport() },
                    onEditClick = {
                        configDialogType = "WHATSAPP"
                        configInputValue = settingsManager.getSupportWhatsapp()
                        showConfigDialog = true
                    },
                    testTag = "menu_whatsapp_support"
                )

                Divider(color = Slate200, modifier = Modifier.padding(vertical = 4.dp))

                // 5. Feature Request
                SupportMenuItem(
                    icon = Icons.Default.Lightbulb,
                    title = "Submit Feature Request",
                    subtitle = "Suggest new modules, printer models or enhancements",
                    onClick = {
                        featTitle = ""
                        featDesc = ""
                        featContact = ""
                        featError = null
                        showFeatureRequestDialog = true
                    },
                    testTag = "menu_feature_request"
                )

                Divider(color = Slate200, modifier = Modifier.padding(vertical = 4.dp))

                // 6. Download for iOS
                SupportMenuItem(
                    icon = Icons.Default.PhoneIphone,
                    title = "Download for iOS",
                    subtitle = if (settingsManager.getIosDownloadUrl().isNotBlank()) settingsManager.getIosDownloadUrl() else "Official Apple App Store download link",
                    onClick = {
                        safeOpenUrl(
                            urlStr = settingsManager.getIosDownloadUrl(),
                            fallbackPrompt = "Configure iOS App Store download URL.",
                            typeKey = "IOS"
                        )
                    },
                    onEditClick = {
                        configDialogType = "IOS"
                        configInputValue = settingsManager.getIosDownloadUrl()
                        showConfigDialog = true
                    },
                    testTag = "menu_download_ios"
                )
            }
        }

        // ==========================================
        // COPYRIGHT FOOTER CARD
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth().testTag("settings_copyright_footer"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate100)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "© 2027 CHOUDHURY POS. All Rights Reserved.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Navy900
                )
                Text(
                    text = "CHOUDHURY POS App • Offline-First Retail & Commercial Management",
                    fontSize = 11.sp,
                    color = Slate600,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // ==========================================
    // ABOUT DIALOG
    // ==========================================
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Gold500),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = Navy900, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("About CHOUDHURY POS", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)
                        Text("Commercial Offline Terminal", fontSize = 11.sp, color = Navy500)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailRow("Application Name", "CHOUDHURY POS App")
                    DetailRow("Version", "2.4.0")
                    DetailRow("Build Code", "42 (Release)")
                    DetailRow("Architecture", "Kotlin Compose Offline-First Room")
                    DetailRow("Developer", "Store Management Solutions / Quickro")
                    DetailRow("Copyright", "© 2027 CHOUDHURY POS")
                    DetailRow("Support Email", settingsManager.getSupportEmail())
                    DetailRow("WhatsApp", settingsManager.getSupportWhatsapp())

                    Surface(
                        color = Emerald50,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Hardware ESC/POS thermal printing, laser barcode scanning, local SQLite database, and multi-branch ledgers run 100% offline without continuous internet.",
                            fontSize = 11.sp,
                            color = Emerald800,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // ==========================================
    // TERMS OF SERVICE DIALOG
    // ==========================================
    if (showTosDialog) {
        AlertDialog(
            onDismissRequest = { showTosDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Gavel, contentDescription = null, tint = Navy900)
                    Text("Terms of Service", fontWeight = FontWeight.Bold, color = Navy900)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. LICENSE & USE:\nChaudhry POS App provides retail point-of-sale, inventory tracking, invoice generation, and customer ledgers. By using this software, you agree to operate within local commerce laws.",
                        fontSize = 12.sp,
                        color = Navy800
                    )
                    Text(
                        text = "2. OFFLINE DATA OWNERSHIP:\nAll business transaction records, inventory databases, customer contact lists, and ledger balances reside securely on the local device storage. The software proprietor is responsible for performing periodic database backups via the Google Drive & local backup tools.",
                        fontSize = 12.sp,
                        color = Navy800
                    )
                    Text(
                        text = "3. SECURITY & CREDENTIALS:\nProprietor credentials (Owner Password and Security Keys) must be maintained confidentially. Disallowed default codes and backdoors are strictly prohibited.",
                        fontSize = 12.sp,
                        color = Navy800
                    )
                    Text(
                        text = "4. HARDWARE COMPATIBILITY:\nCompatible ESC/POS Bluetooth and Network thermal printers (58mm/80mm) are supported. Printing operations do not alter sale totals.",
                        fontSize = 12.sp,
                        color = Navy800
                    )
                    Text(
                        text = "© 2027 CHOUDHURY POS. All Rights Reserved.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate600
                    )

                    if (settingsManager.getTermsOfServiceUrl().isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                safeOpenUrl(settingsManager.getTermsOfServiceUrl(), "", "TOS")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Official Online Terms")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTosDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900)
                ) {
                    Text("I Understand")
                }
            }
        )
    }

    // ==========================================
    // FEATURE REQUEST DIALOG
    // ==========================================
    if (showFeatureRequestDialog) {
        AlertDialog(
            onDismissRequest = { showFeatureRequestDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Gold600)
                    Text("Submit Feature Request", fontWeight = FontWeight.Bold, color = Navy900)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Share suggestions or requested integrations with our development team:",
                        fontSize = 12.sp,
                        color = Navy600
                    )

                    OutlinedTextField(
                        value = featTitle,
                        onValueChange = {
                            featTitle = it
                            featError = null
                        },
                        label = { Text("Feature Title") },
                        placeholder = { Text("e.g. Weighing Scale Integration") },
                        singleLine = true,
                        isError = featError != null,
                        modifier = Modifier.fillMaxWidth().testTag("feat_title_input")
                    )

                    OutlinedTextField(
                        value = featDesc,
                        onValueChange = {
                            featDesc = it
                            featError = null
                        },
                        label = { Text("Feature Description") },
                        placeholder = { Text("Describe how this feature should work...") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth().testTag("feat_desc_input")
                    )

                    OutlinedTextField(
                        value = featContact,
                        onValueChange = { featContact = it },
                        label = { Text("Your Contact / Email (Optional)") },
                        placeholder = { Text("name@example.com or phone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("feat_contact_input")
                    )

                    if (featError != null) {
                        Text(featError!!, color = Rose600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanTitle = featTitle.trim()
                        val cleanDesc = featDesc.trim()
                        if (cleanTitle.isBlank()) {
                            featError = "Please enter a feature title."
                        } else if (cleanDesc.isBlank()) {
                            featError = "Please describe the requested feature."
                        } else {
                            settingsManager.saveFeatureRequest(
                                FeatureRequestItem(
                                    title = cleanTitle,
                                    description = cleanDesc,
                                    contactInfo = featContact.trim()
                                )
                            )
                            showFeatureRequestDialog = false
                            Toast.makeText(context, "Feature request submitted successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("btn_submit_feature_request")
                ) {
                    Text("Submit Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeatureRequestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // CONFIGURATION DIALOG FOR PLACEHOLDERS
    // ==========================================
    if (showConfigDialog) {
        val (dialogTitle, labelText, placeholderText) = when (configDialogType) {
            "EMAIL" -> Triple("Configure Support Email", "Support Email Address", "support@yourdomain.com")
            "WHATSAPP" -> Triple("Configure WhatsApp Support", "WhatsApp Number with Country Code", "923080018035")
            "PORTAL" -> Triple("Configure Portal URL", "Official Web Portal URL", "https://portal.chaudhrypos.com")
            "IOS" -> Triple("Configure iOS Download URL", "Apple App Store URL", "https://apps.apple.com/app/...")
            "WINDOWS" -> Triple("Configure Windows Download URL", "Windows Package URL", "https://chaudhrypos.com/download/windows")
            else -> Triple("Configure Setting", "URL / Value", "https://...")
        }

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Text(dialogTitle, fontWeight = FontWeight.Bold, color = Navy900)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter or update the official destination. Broken or fabricated links are not permitted.",
                        fontSize = 12.sp,
                        color = Navy600
                    )
                    OutlinedTextField(
                        value = configInputValue,
                        onValueChange = { configInputValue = it },
                        label = { Text(labelText) },
                        placeholder = { Text(placeholderText) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("config_dialog_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = configInputValue.trim()
                        when (configDialogType) {
                            "EMAIL" -> settingsManager.setSupportEmail(clean)
                            "WHATSAPP" -> settingsManager.setSupportWhatsapp(clean)
                            "PORTAL" -> settingsManager.setPortalUrl(clean)
                            "IOS" -> settingsManager.setIosDownloadUrl(clean)
                            "WINDOWS" -> settingsManager.setWindowsDownloadUrl(clean)
                        }
                        showConfigDialog = false
                        Toast.makeText(context, "Configuration updated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SupportMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                color = Slate100,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Navy800, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = Navy900)
                Text(subtitle, fontSize = 11.sp, color = Slate600, maxLines = 1)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onEditClick != null) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Config", tint = Slate500, modifier = Modifier.size(16.dp))
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 11.5.sp, color = Slate600, modifier = Modifier.weight(0.4f))
        Text(value, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Navy900, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
    }
}
