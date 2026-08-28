package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.StoreSettings
import com.example.data.entity.User
import com.example.ui.theme.*

data class NavChildItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val testTag: String,
    val minRole: String = "EMPLOYEE"
)

data class NavFolder(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val bgColor: Color,
    val items: List<NavChildItem>
)

@Composable
fun SidebarNavigationDrawerContent(
    activeUser: User?,
    storeSettings: StoreSettings?,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Default State: All folders are collapsed by default as required
    var expandedFolderIds by remember { mutableStateOf(setOf<String>()) }

    fun toggleFolder(folderId: String) {
        expandedFolderIds = if (expandedFolderIds.contains(folderId)) {
            expandedFolderIds - folderId
        } else {
            expandedFolderIds + folderId
        }
    }

    val userRole = activeUser?.role?.uppercase() ?: "SUPER_ADMIN"

    fun isItemAllowed(minRole: String): Boolean {
        return when (minRole) {
            "SUPER_ADMIN" -> userRole == "SUPER_ADMIN"
            "ADMIN" -> userRole == "SUPER_ADMIN" || userRole == "ADMIN"
            "MANAGER" -> userRole == "SUPER_ADMIN" || userRole == "ADMIN" || userRole == "MANAGER"
            "CASHIER" -> userRole == "SUPER_ADMIN" || userRole == "ADMIN" || userRole == "MANAGER" || userRole == "CASHIER"
            else -> true
        }
    }

    // 6 Main Folders with complete child structures
    val folders = listOf(
        // 1. STORE & BUSINESS
        NavFolder(
            id = "store_business",
            title = "Store & Business",
            icon = Icons.Default.Storefront,
            color = Blue600,
            bgColor = Blue50,
            items = listOf(
                NavChildItem(
                    title = "Store Information & Profile",
                    subtitle = "Name, NTN, contact & tax settings",
                    icon = Icons.Default.Store,
                    route = "setup",
                    testTag = "drawer_item_store_info"
                ),
                NavChildItem(
                    title = "Business & Branch Management",
                    subtitle = "Multi-branch & store identity",
                    icon = Icons.Default.Business,
                    route = "store_management",
                    testTag = "drawer_item_business_management"
                ),
                NavChildItem(
                    title = "Store Access & Privileges",
                    subtitle = "Granular cashier & manager access",
                    icon = Icons.Default.Security,
                    route = "access_management",
                    testTag = "drawer_item_store_access"
                ),
                NavChildItem(
                    title = "Business Identity",
                    subtitle = "Store branding, logos & headers",
                    icon = Icons.Default.Badge,
                    route = "setup",
                    testTag = "drawer_item_business_identity"
                ),
                NavChildItem(
                    title = "Owner Information",
                    subtitle = "Proprietor details & CNIC",
                    icon = Icons.Default.Person,
                    route = "setup",
                    testTag = "drawer_item_owner_info"
                ),
                NavChildItem(
                    title = "Store Contact Details",
                    subtitle = "Phone, WhatsApp & address",
                    icon = Icons.Default.Phone,
                    route = "setup",
                    testTag = "drawer_item_contact_info"
                ),
                NavChildItem(
                    title = "Website / Online Information",
                    subtitle = "Online store & digital receipts",
                    icon = Icons.Default.Language,
                    route = "setup",
                    testTag = "drawer_item_online_info"
                )
            )
        ),

        // 2. SALES & OPERATIONS
        NavFolder(
            id = "sales_operations",
            title = "Sales & Operations",
            icon = Icons.Default.PointOfSale,
            color = Emerald600,
            bgColor = Emerald50,
            items = listOf(
                NavChildItem(
                    title = "Sales POS Terminal",
                    subtitle = "Fast barcode & manual checkout",
                    icon = Icons.Default.PointOfSale,
                    route = "pos",
                    testTag = "drawer_item_pos"
                ),
                NavChildItem(
                    title = "Invoices & Receipts",
                    subtitle = "Sales history, search & thermal print",
                    icon = Icons.Default.ReceiptLong,
                    route = "invoice",
                    testTag = "drawer_item_invoices"
                ),
                NavChildItem(
                    title = "Purchases & Stock In",
                    subtitle = "Supplier bills & inventory receipts",
                    icon = Icons.Default.ShoppingBag,
                    route = "purchases",
                    testTag = "drawer_item_purchases"
                ),
                NavChildItem(
                    title = "Inventory & Products",
                    subtitle = "Stock tracking, SKU & pricing",
                    icon = Icons.Default.Inventory2,
                    route = "inventory",
                    testTag = "drawer_item_inventory"
                ),
                NavChildItem(
                    title = "Customer Ledger",
                    subtitle = "Khata balances & payment records",
                    icon = Icons.Default.People,
                    route = "customers",
                    testTag = "drawer_item_customers"
                ),
                NavChildItem(
                    title = "Supplier Accounts",
                    subtitle = "Vendor balances & purchase orders",
                    icon = Icons.Default.LocalShipping,
                    route = "suppliers",
                    testTag = "drawer_item_suppliers"
                ),
                NavChildItem(
                    title = "Daily Cash Closing",
                    subtitle = "Drawer balancing & session summary",
                    icon = Icons.Default.LockClock,
                    route = "closing",
                    testTag = "drawer_item_closing"
                )
            )
        ),

        // 3. STAFF MANAGEMENT
        NavFolder(
            id = "staff_management",
            title = "Staff Management",
            icon = Icons.Default.Badge,
            color = Purple600,
            bgColor = Purple100.copy(alpha = 0.5f),
            items = listOf(
                NavChildItem(
                    title = "Staff Attendance",
                    subtitle = "Check-in/out, logs & biometric shifts",
                    icon = Icons.Default.Badge,
                    route = "attendance",
                    testTag = "drawer_item_attendance"
                ),
                NavChildItem(
                    title = "Employee Management",
                    subtitle = "Staff directory, active status & details",
                    icon = Icons.Default.ManageAccounts,
                    route = "users",
                    testTag = "drawer_item_employee_management",
                    minRole = "ADMIN"
                ),
                NavChildItem(
                    title = "Employee Payroll",
                    subtitle = "Salaries, advances & shift totals",
                    icon = Icons.Default.Payments,
                    route = "attendance",
                    testTag = "drawer_item_employee_payroll",
                    minRole = "ADMIN"
                ),
                NavChildItem(
                    title = "Staff Roles",
                    subtitle = "Permissions & duty assignments",
                    icon = Icons.Default.AssignmentInd,
                    route = "users",
                    testTag = "drawer_item_staff_roles",
                    minRole = "ADMIN"
                )
            )
        ),

        // 4. REPORTS & FINANCE
        NavFolder(
            id = "reports_finance",
            title = "Reports & Finance",
            icon = Icons.Default.Assessment,
            color = Rose600,
            bgColor = Rose100.copy(alpha = 0.5f),
            items = listOf(
                NavChildItem(
                    title = "Financial Reports & P&L",
                    subtitle = "Sales revenue, profit & analytics",
                    icon = Icons.Default.Assessment,
                    route = "reports",
                    testTag = "drawer_item_reports",
                    minRole = "ADMIN"
                ),
                NavChildItem(
                    title = "Sales Reports",
                    subtitle = "Daily, weekly & periodic sales summary",
                    icon = Icons.Default.QueryStats,
                    route = "reports",
                    testTag = "drawer_item_sales_reports",
                    minRole = "ADMIN"
                ),
                NavChildItem(
                    title = "Purchase Reports",
                    subtitle = "Procurement & vendor summaries",
                    icon = Icons.Default.ShoppingBag,
                    route = "reports",
                    testTag = "drawer_item_purchase_reports",
                    minRole = "ADMIN"
                ),
                NavChildItem(
                    title = "Stock Reports",
                    subtitle = "Inventory valuation & low-stock alerts",
                    icon = Icons.Default.Inventory,
                    route = "reports",
                    testTag = "drawer_item_stock_reports",
                    minRole = "ADMIN"
                ),
                NavChildItem(
                    title = "Expense Reports",
                    subtitle = "Operating costs & overheads",
                    icon = Icons.Default.Receipt,
                    route = "reports",
                    testTag = "drawer_item_expense_reports",
                    minRole = "ADMIN"
                ),
                NavChildItem(
                    title = "Customer Ledger Reports",
                    subtitle = "Receivables & khata credit aging",
                    icon = Icons.Default.AccountBalanceWallet,
                    route = "reports",
                    testTag = "drawer_item_cust_reports",
                    minRole = "ADMIN"
                ),
                NavChildItem(
                    title = "Supplier Ledger Reports",
                    subtitle = "Vendor payables & settlement history",
                    icon = Icons.Default.LocalShipping,
                    route = "reports",
                    testTag = "drawer_item_supp_reports",
                    minRole = "ADMIN"
                )
            )
        ),

        // 5. ADMINISTRATION
        NavFolder(
            id = "administration",
            title = "Administration",
            icon = Icons.Default.AdminPanelSettings,
            color = Gold600,
            bgColor = Gold50,
            items = listOf(
                NavChildItem(
                    title = "Cashier Management",
                    subtitle = "Cashier roster, active toggle & name editor",
                    icon = Icons.Default.PointOfSale,
                    route = "cashier_management",
                    testTag = "drawer_item_cashier_management",
                    minRole = "ADMIN"
                ),
                NavChildItem(
                    title = "Staff & User Roles",
                    subtitle = "Cashiers, managers & credentials",
                    icon = Icons.Default.ManageAccounts,
                    route = "users",
                    testTag = "drawer_item_staff_users",
                    minRole = "ADMIN"
                ),
                NavChildItem(
                    title = "Admin Management",
                    subtitle = "Administrative privileges & accounts",
                    icon = Icons.Default.SupervisorAccount,
                    route = "users",
                    testTag = "drawer_item_admin_management",
                    minRole = "ADMIN"
                ),
                NavChildItem(
                    title = "Super Admin Controls",
                    subtitle = "Master system authority & database controls",
                    icon = Icons.Default.Shield,
                    route = "master_saas",
                    testTag = "drawer_item_super_admin_controls",
                    minRole = "SUPER_ADMIN"
                ),
                NavChildItem(
                    title = "Permissions & Access Control",
                    subtitle = "Granular role access policy matrix",
                    icon = Icons.Default.LockOpen,
                    route = "access_management",
                    testTag = "drawer_item_permissions_access",
                    minRole = "ADMIN"
                ),
                NavChildItem(
                    title = "SaaS Master Control",
                    subtitle = "Store activation & instance locks",
                    icon = Icons.Default.AdminPanelSettings,
                    route = "master_saas",
                    testTag = "drawer_item_saas_master",
                    minRole = "SUPER_ADMIN"
                ),
                NavChildItem(
                    title = "License Activation",
                    subtitle = "Hardware key & license verification",
                    icon = Icons.Default.VpnKey,
                    route = "activation",
                    testTag = "drawer_item_activation",
                    minRole = "ADMIN"
                ),
                NavChildItem(
                    title = "Audit Activity Logs",
                    subtitle = "Immutable security actions log",
                    icon = Icons.Default.History,
                    route = "logs",
                    testTag = "drawer_item_logs",
                    minRole = "ADMIN"
                ),
                NavChildItem(
                    title = "Recycle Bin",
                    subtitle = "Soft-deleted records recovery",
                    icon = Icons.Default.DeleteSweep,
                    route = "recycle_bin",
                    testTag = "drawer_item_recycle_bin",
                    minRole = "ADMIN"
                ),
                NavChildItem(
                    title = "Developer & Diagnostics",
                    subtitle = "Room DB integrity & device logs",
                    icon = Icons.Default.DeveloperMode,
                    route = "dev_panel",
                    testTag = "drawer_item_dev_panel",
                    minRole = "SUPER_ADMIN"
                )
            )
        ),

        // 6. SETTINGS & CUSTOMIZATION
        NavFolder(
            id = "settings_customization",
            title = "Settings & Customization",
            icon = Icons.Default.Tune,
            color = Teal600,
            bgColor = Teal50,
            items = listOf(
                NavChildItem(
                    title = "Appearance & Theme",
                    subtitle = "Light, Dark, Black & System theme modes",
                    icon = Icons.Default.Palette,
                    route = "settings",
                    testTag = "drawer_item_appearance"
                ),
                NavChildItem(
                    title = "Accent Color Selection",
                    subtitle = "Royal Blue, Emerald, Purple & more",
                    icon = Icons.Default.ColorLens,
                    route = "settings",
                    testTag = "drawer_item_accent_color"
                ),
                NavChildItem(
                    title = "Text & Font Scaling",
                    subtitle = "Adjust size (85%-130%) & typography fonts",
                    icon = Icons.Default.TextFields,
                    route = "settings",
                    testTag = "drawer_item_text_font"
                ),
                NavChildItem(
                    title = "Store & General Settings",
                    subtitle = "Receipt format, audio & store parameters",
                    icon = Icons.Default.Settings,
                    route = "settings",
                    testTag = "drawer_item_settings"
                ),
                NavChildItem(
                    title = "Backup & Restore",
                    subtitle = "Local SQLite database export & cloud backup",
                    icon = Icons.Default.Backup,
                    route = "settings",
                    testTag = "drawer_item_backup"
                ),
                NavChildItem(
                    title = "Security Settings",
                    subtitle = "Biometric authentication & PIN protection",
                    icon = Icons.Default.Fingerprint,
                    route = "settings",
                    testTag = "drawer_item_security"
                )
            )
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Drawer Header with Professional Navy Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Navy900)
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShopLogoAvatar(
                        logoUri = storeSettings?.logoUri,
                        size = 44.dp,
                        shape = RoundedCornerShape(10.dp),
                        borderColor = Gold400,
                        borderWidth = 1.5.dp
                    )
                    Column {
                        Text(
                            text = storeSettings?.appDisplayName?.ifBlank { storeSettings?.storeName } ?: "CH UMER POS",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = storeSettings?.tagline?.ifBlank { "Tel: ${storeSettings?.phone ?: "03080018035"}" } ?: "SMART | FAST | RELIABLE",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Gold400
                        )
                    }
                }

                // Current User & Active Role Chip
                Surface(
                    color = Navy800,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Gold400,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = activeUser?.fullName?.ifBlank { activeUser.username } ?: "Muhammad Umer",
                                    fontSize = 12.5.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "@${activeUser?.username ?: "admin"}",
                                    fontSize = 10.5.sp,
                                    color = Navy400
                                )
                            }
                        }
                        Surface(
                            color = Gold500,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = userRole,
                                fontSize = 10.sp,
                                color = Navy900,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Collapsible Folder List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            folders.forEach { folder ->
                val visibleItems = folder.items.filter { isItemAllowed(it.minRole) }

                // Only show folder if user has access to at least one child item
                if (visibleItems.isNotEmpty()) {
                    val isExpanded = expandedFolderIds.contains(folder.id)
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isExpanded) 90f else 0f,
                        label = "chevron_rotation"
                    )

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpanded) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isExpanded) {
                                folder.color.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("folder_card_${folder.id}")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Main Category Header Row (Tap to expand/collapse)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { toggleFolder(folder.id) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("folder_header_${folder.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Category Icon Pill
                                Surface(
                                    color = folder.bgColor,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = folder.icon,
                                            contentDescription = folder.title,
                                            tint = folder.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Category Name with Strong Contrast
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folder.title,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = 0.2.sp
                                    )
                                    Text(
                                        text = "${visibleItems.size} options",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Animated Chevron Indicator (> when collapsed, v when opened)
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = if (isExpanded) folder.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .rotate(rotationAngle)
                                )
                            }

                            // Child Options Container (Visible only when expanded)
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 8.dp, bottom = 8.dp, top = 2.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Divider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    visibleItems.forEach { child ->
                                        Surface(
                                            onClick = {
                                                onCloseDrawer()
                                                onNavigate(child.route)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.Transparent,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag(child.testTag)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // Child Icon with sharp high-contrast tint
                                                Surface(
                                                    color = folder.bgColor.copy(alpha = 0.7f),
                                                    shape = CircleShape,
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = child.icon,
                                                            contentDescription = child.title,
                                                            tint = folder.color,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = child.title,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        lineHeight = 16.sp
                                                    )
                                                    Text(
                                                        text = child.subtitle,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }

                                                Icon(
                                                    imageVector = Icons.Default.ArrowForwardIos,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Offline Status Badge in Footer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("drawer_offline_footer"),
            shape = RoundedCornerShape(10.dp),
            color = Emerald50,
            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald300)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Emerald600,
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "✓ Offline Mode: 100% Active",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald900
                    )
                    Text(
                        text = "POS data safely saved locally in Room DB",
                        fontSize = 10.5.sp,
                        color = Emerald800
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
