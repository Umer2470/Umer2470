package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.User
import com.example.ui.components.AppHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

enum class CashierFilterTab {
    ALL,
    ACTIVE_ONLY,
    INACTIVE
}

@Composable
fun UserManagementScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val users by viewModel.users.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val activeCashierName by viewModel.activeCashierName.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var showQuickRenameDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<User?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(CashierFilterTab.ALL) }
    var bannerSuccessMsg by remember { mutableStateOf<String?>(null) }

    val isAdmin = remember(activeUser) {
        val role = activeUser?.role?.uppercase() ?: "SUPER_ADMIN"
        role in listOf("SUPER_ADMIN", "ADMIN", "SUPER ADMIN", "ADMINISTRATOR") || activeUser == null
    }

    val filteredUsers = remember(users, searchQuery, selectedTab) {
        users.filter { user ->
            val matchesSearch = searchQuery.isBlank() ||
                    user.fullName.contains(searchQuery, ignoreCase = true) ||
                    user.username.contains(searchQuery, ignoreCase = true) ||
                    user.role.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (selectedTab) {
                CashierFilterTab.ALL -> true
                CashierFilterTab.ACTIVE_ONLY -> user.isActive
                CashierFilterTab.INACTIVE -> !user.isActive
            }

            matchesSearch && matchesTab
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Cashier & Staff Management",
                subtitle = "${users.count { it.isActive }} Active • ${users.size} Total Staff",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = {
                        selectedUserForEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = Navy900,
                    contentColor = Gold500,
                    modifier = Modifier.testTag("add_user_fab")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Cashier / Staff")
                }
            }
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Success / Notice Notification
            if (bannerSuccessMsg != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Emerald100),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald700, modifier = Modifier.size(18.dp))
                            Text(bannerSuccessMsg!!, fontSize = 12.sp, color = Emerald800, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(
                            onClick = { bannerSuccessMsg = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Emerald700, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Current Active Cashier Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = "Cashier Badge",
                                    tint = Navy900,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(22.dp)
                                )
                            }
                            Column {
                                Text("Current Active POS Cashier", fontSize = 11.sp, color = Slate300)
                                Text(
                                    text = activeCashierName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatusBadge(
                                text = "POS ACTIVE",
                                backgroundColor = Emerald100,
                                textColor = Emerald700
                            )
                            IconButton(
                                onClick = { showQuickRenameDialog = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("quick_rename_active_cashier_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename Active Cashier", tint = Gold400, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This name automatically appears on all new sales, POS thermal receipts, and official tax invoices.",
                        fontSize = 11.sp,
                        color = Slate300
                    )
                }
            }

            // Quick Add Cashier Button (Top Bar Action for immediate discovery)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cashier Roster (${filteredUsers.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )

                if (isAdmin) {
                    Button(
                        onClick = {
                            selectedUserForEdit = null
                            showAddEditDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_cashier_top_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New Cashier", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search cashiers by name, username, or role...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Navy700) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cashier_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Navy900,
                    unfocusedBorderColor = Slate300,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Tabs (All, Active, Inactive)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedTab == CashierFilterTab.ALL,
                    onClick = { selectedTab = CashierFilterTab.ALL },
                    label = { Text("All (${users.size})", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_all_cashiers")
                )
                FilterChip(
                    selected = selectedTab == CashierFilterTab.ACTIVE_ONLY,
                    onClick = { selectedTab = CashierFilterTab.ACTIVE_ONLY },
                    label = { Text("Active (${users.count { it.isActive }})", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_active_cashiers")
                )
                FilterChip(
                    selected = selectedTab == CashierFilterTab.INACTIVE,
                    onClick = { selectedTab = CashierFilterTab.INACTIVE },
                    label = { Text("Inactive (${users.count { !it.isActive }})", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_inactive_cashiers")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = Slate400, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No cashiers found in this category." else "No cashiers match '$searchQuery'",
                            color = Navy600,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredUsers, key = { it.id }) { user ->
                        val isCashierActive = activeCashierName.equals(user.fullName, ignoreCase = true) ||
                                (user.fullName.isBlank() && activeCashierName.equals(user.username, ignoreCase = true))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("user_card_${user.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = user.fullName.ifBlank { user.username },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Navy900
                                            )
                                            if (isCashierActive) {
                                                StatusBadge(
                                                    text = "Active POS",
                                                    backgroundColor = Emerald100,
                                                    textColor = Emerald700
                                                )
                                            }
                                            StatusBadge(
                                                text = if (user.isActive) "Active" else "Inactive",
                                                backgroundColor = if (user.isActive) Blue100 else Rose100,
                                                textColor = if (user.isActive) Blue600 else Rose600
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Username: @${user.username} • Role: ${user.role}",
                                            fontSize = 12.sp,
                                            color = Navy600
                                        )
                                    }

                                    if (isAdmin) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Toggle Status Button
                                            IconButton(
                                                onClick = {
                                                    viewModel.toggleUserStatus(user) {
                                                        bannerSuccessMsg = "Cashier status updated to ${if (!user.isActive) "Active" else "Inactive"}"
                                                    }
                                                },
                                                modifier = Modifier.testTag("toggle_status_user_${user.id}")
                                            ) {
                                                Icon(
                                                    imageVector = if (user.isActive) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                                    contentDescription = "Toggle Status",
                                                    tint = if (user.isActive) Emerald600 else Slate400,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }

                                            // Edit Button
                                            IconButton(
                                                onClick = {
                                                    selectedUserForEdit = user
                                                    showAddEditDialog = true
                                                },
                                                modifier = Modifier.testTag("edit_user_${user.id}")
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Cashier", tint = Navy600)
                                            }

                                            // Delete Button
                                            if (users.size > 1) {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteUser(user.id)
                                                        bannerSuccessMsg = "Cashier '${user.fullName.ifBlank { user.username }}' removed."
                                                    },
                                                    modifier = Modifier.testTag("delete_user_${user.id}")
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Cashier", tint = Rose600)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Slate100)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when {
                                            !user.isActive -> "Deactivated • Cannot be assigned to POS sales"
                                            isCashierActive -> "Currently assigned to all new sales & invoices"
                                            else -> "Ready for POS counter assignment"
                                        },
                                        fontSize = 11.sp,
                                        color = when {
                                            !user.isActive -> Rose600
                                            isCashierActive -> Emerald700
                                            else -> Slate400
                                        }
                                    )

                                    Button(
                                        onClick = {
                                            val targetName = user.fullName.ifBlank { user.username }
                                            viewModel.setActiveCashierName(targetName, user)
                                            bannerSuccessMsg = "Active POS Cashier set to '$targetName'"
                                        },
                                        enabled = user.isActive && !isCashierActive,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isCashierActive) Emerald600 else Navy900,
                                            disabledContainerColor = if (isCashierActive) Emerald100 else Slate200,
                                            disabledContentColor = if (isCashierActive) Emerald700 else Slate400
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("select_cashier_${user.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isCashierActive) Icons.Default.Check else Icons.Default.HowToReg,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isCashierActive) "Selected Active" else "Set as Active",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Rename Active Cashier Dialog
        if (showQuickRenameDialog) {
            var newName by remember { mutableStateOf(activeCashierName) }
            var renameError by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { showQuickRenameDialog = false },
                title = {
                    Text(
                        text = "Edit Active Cashier Name",
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Update the active cashier name immediately. This will be stamped on all upcoming sales & printed receipts.",
                            fontSize = 12.sp,
                            color = Navy600
                        )

                        OutlinedTextField(
                            value = newName,
                            onValueChange = {
                                newName = it
                                renameError = null
                            },
                            label = { Text("Cashier Full Name *") },
                            placeholder = { Text("e.g. Muhammad Umer") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_cashier_name_input")
                        )

                        if (renameError != null) {
                            Text(text = renameError!!, color = Rose600, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newName.isBlank()) {
                                renameError = "Cashier Name cannot be empty."
                            } else {
                                val trimmed = newName.trim()
                                val matchedUser = users.find {
                                    it.fullName.equals(activeCashierName, ignoreCase = true) ||
                                    it.username.equals(activeCashierName, ignoreCase = true)
                                }
                                if (matchedUser != null) {
                                    val updatedUser = matchedUser.copy(fullName = trimmed)
                                    viewModel.saveUser(updatedUser) {
                                        viewModel.setActiveCashierName(trimmed, updatedUser)
                                        bannerSuccessMsg = "Active Cashier updated to '$trimmed'"
                                        showQuickRenameDialog = false
                                    }
                                } else {
                                    viewModel.setActiveCashierName(trimmed)
                                    bannerSuccessMsg = "Active Cashier updated to '$trimmed'"
                                    showQuickRenameDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        modifier = Modifier.testTag("save_quick_cashier_name_button")
                    ) {
                        Text("Apply Name")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showQuickRenameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Add / Edit Full Cashier Profile Dialog
        if (showAddEditDialog) {
            var fullName by remember { mutableStateOf(selectedUserForEdit?.fullName ?: "") }
            var username by remember { mutableStateOf(selectedUserForEdit?.username ?: "") }
            var pin by remember { mutableStateOf(selectedUserForEdit?.pinHash ?: "") }
            var role by remember { mutableStateOf(selectedUserForEdit?.role ?: "CASHIER") }
            var isActive by remember { mutableStateOf(selectedUserForEdit?.isActive ?: true) }
            var setAsActivePosCashier by remember {
                mutableStateOf(
                    selectedUserForEdit != null && (
                        activeCashierName.equals(selectedUserForEdit?.fullName, ignoreCase = true) ||
                        activeCashierName.equals(selectedUserForEdit?.username, ignoreCase = true)
                    )
                )
            }
            var errorMsg by remember { mutableStateOf<String?>(null) }

            val roles = listOf("SUPER_ADMIN", "ADMIN", "SUPERVISOR", "CASHIER")

            AlertDialog(
                onDismissRequest = { showAddEditDialog = false },
                title = {
                    Text(
                        text = if (selectedUserForEdit == null) "Add New Cashier / Staff" else "Edit Cashier Profile",
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                errorMsg = null
                            },
                            label = { Text("Cashier Full Name * (Mandatory)") },
                            placeholder = { Text("e.g. Muhammad Umer") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("user_fullname_input")
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                errorMsg = null
                            },
                            label = { Text("Username / Login ID *") },
                            placeholder = { Text("e.g. umer_pos") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("user_username_input")
                        )

                        OutlinedTextField(
                            value = pin,
                            onValueChange = {
                                pin = it
                                errorMsg = null
                            },
                            label = { Text("Terminal PIN / Password *") },
                            placeholder = { Text("e.g. 1234") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("user_pin_input")
                        )

                        Text("Assigned Role & Permissions:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy900)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            roles.forEach { r ->
                                FilterChip(
                                    selected = role.equals(r, ignoreCase = true),
                                    onClick = { role = r },
                                    label = { Text(r, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("role_chip_$r")
                                )
                            }
                        }

                        // Status Switch (Active / Inactive)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate100,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Account Status", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy900)
                                    Text(if (isActive) "Active (Can operate POS)" else "Inactive (Disabled)", fontSize = 11.sp, color = if (isActive) Emerald700 else Rose600)
                                }
                                Switch(
                                    checked = isActive,
                                    onCheckedChange = { isActive = it },
                                    modifier = Modifier.testTag("user_active_switch")
                                )
                            }
                        }

                        // Set As Active POS Cashier Option
                        if (isActive) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { setAsActivePosCashier = !setAsActivePosCashier }
                            ) {
                                Checkbox(
                                    checked = setAsActivePosCashier,
                                    onCheckedChange = { setAsActivePosCashier = it },
                                    modifier = Modifier.testTag("set_active_pos_checkbox")
                                )
                                Text("Assign as active POS cashier immediately", fontSize = 12.sp, color = Navy900)
                            }
                        }

                        if (errorMsg != null) {
                            Text(text = errorMsg!!, color = Rose600, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (fullName.isBlank()) {
                                errorMsg = "Cashier Full Name is required."
                            } else if (username.isBlank() || pin.isBlank()) {
                                errorMsg = "Username and Terminal PIN are required."
                            } else {
                                val userToSave = (selectedUserForEdit ?: User(
                                    username = username.trim(),
                                    pinHash = pin.trim()
                                )).copy(
                                    fullName = fullName.trim(),
                                    username = username.trim(),
                                    pinHash = pin.trim(),
                                    role = role,
                                    isActive = isActive
                                )

                                viewModel.saveUser(userToSave) {
                                    val assignedName = fullName.trim().ifBlank { username.trim() }
                                    if (setAsActivePosCashier && isActive) {
                                        viewModel.setActiveCashierName(assignedName, userToSave)
                                    } else if (selectedUserForEdit != null &&
                                        (activeCashierName.equals(selectedUserForEdit?.fullName, ignoreCase = true) ||
                                         activeCashierName.equals(selectedUserForEdit?.username, ignoreCase = true))) {
                                        if (isActive) {
                                            viewModel.setActiveCashierName(assignedName, userToSave)
                                        }
                                    }
                                    bannerSuccessMsg = "Cashier '${assignedName}' saved successfully."
                                    showAddEditDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        modifier = Modifier.testTag("save_user_button")
                    ) {
                        Text(if (selectedUserForEdit == null) "Create Cashier" else "Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddEditDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

