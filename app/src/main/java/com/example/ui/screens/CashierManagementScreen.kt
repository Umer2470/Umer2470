package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

enum class CashierFilterStatus {
    ALL,
    ACTIVE,
    INACTIVE
}

@Composable
fun CashierManagementScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val users by viewModel.users.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val activeCashierName by viewModel.activeCashierName.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf(CashierFilterStatus.ALL) }

    // Dialog state for Editing Cashier Name & details
    var userToEditName by remember { mutableStateOf<User?>(null) }
    var showEditNameDialog by remember { mutableStateOf(false) }

    // Dialog state for Adding New Cashier
    var showAddCashierDialog by remember { mutableStateOf(false) }

    // Alert feedback banner message
    var bannerMessage by remember { mutableStateOf<String?>(null) }

    val isAdmin = remember(activeUser) {
        val role = activeUser?.role?.uppercase() ?: "SUPER_ADMIN"
        role in listOf("SUPER_ADMIN", "ADMIN", "SUPER ADMIN", "ADMINISTRATOR", "MANAGER") || activeUser == null
    }

    // Filter cashiers
    val filteredCashiers = remember(users, searchQuery, filterStatus) {
        users.filter { user ->
            val matchesSearch = searchQuery.isBlank() ||
                    user.fullName.contains(searchQuery, ignoreCase = true) ||
                    user.username.contains(searchQuery, ignoreCase = true) ||
                    user.role.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (filterStatus) {
                CashierFilterStatus.ALL -> true
                CashierFilterStatus.ACTIVE -> user.isActive
                CashierFilterStatus.INACTIVE -> !user.isActive
            }

            matchesSearch && matchesFilter
        }
    }

    val activeCount = remember(users) { users.count { it.isActive } }
    val inactiveCount = remember(users) { users.count { !it.isActive } }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Cashier Management",
                subtitle = "Administration • $activeCount Active / ${users.size} Total Cashiers",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showAddCashierDialog = true },
                    containerColor = Navy900,
                    contentColor = Gold500,
                    modifier = Modifier.testTag("add_cashier_fab")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add New Cashier")
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
            // Success Feedback Banner
            if (bannerMessage != null) {
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
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald700, modifier = Modifier.size(18.dp))
                            Text(bannerMessage!!, fontSize = 12.sp, color = Emerald800, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(
                            onClick = { bannerMessage = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Emerald700, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Active POS Operator Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = "POS Cashier Icon",
                                tint = Navy900,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Column {
                            Text("Current Active POS Operator", fontSize = 11.sp, color = Slate300)
                            Text(
                                text = activeCashierName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    StatusBadge(
                        text = "POS ASSIGNED",
                        backgroundColor = Emerald100,
                        textColor = Emerald700
                    )
                }
            }

            // Search Bar & Filter Tabs
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search cashiers by name or username...", fontSize = 12.sp) },
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
                    .testTag("cashier_search_field"),
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

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterStatus == CashierFilterStatus.ALL,
                    onClick = { filterStatus = CashierFilterStatus.ALL },
                    label = { Text("All (${users.size})", fontSize = 11.sp) },
                    modifier = Modifier.testTag("filter_all_cashiers")
                )
                FilterChip(
                    selected = filterStatus == CashierFilterStatus.ACTIVE,
                    onClick = { filterStatus = CashierFilterStatus.ACTIVE },
                    label = { Text("Active ($activeCount)", fontSize = 11.sp) },
                    modifier = Modifier.testTag("filter_active_cashiers")
                )
                FilterChip(
                    selected = filterStatus == CashierFilterStatus.INACTIVE,
                    onClick = { filterStatus = CashierFilterStatus.INACTIVE },
                    label = { Text("Inactive ($inactiveCount)", fontSize = 11.sp) },
                    modifier = Modifier.testTag("filter_inactive_cashiers")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cashiers List View
            if (filteredCashiers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = Slate400, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No cashiers found." else "No results for '$searchQuery'",
                            color = Navy600,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("cashier_list_view"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCashiers, key = { it.id }) { cashier ->
                        val isPosActive = activeCashierName.equals(cashier.fullName, ignoreCase = true) ||
                                (cashier.fullName.isBlank() && activeCashierName.equals(cashier.username, ignoreCase = true))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cashier_item_${cashier.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                // Top Row: Avatar, Name, Status, and Action Controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Cashier Avatar
                                        Box {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (cashier.isActive) Navy100 else Slate200,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = if (cashier.isActive) Navy900 else Slate400,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                            // Status indicator dot
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(if (cashier.isActive) Emerald600 else Rose500)
                                                    .align(Alignment.BottomEnd)
                                            )
                                        }

                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = cashier.fullName.ifBlank { cashier.username },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Navy900
                                                )
                                                if (isPosActive) {
                                                    StatusBadge(
                                                        text = "Active POS",
                                                        backgroundColor = Emerald100,
                                                        textColor = Emerald700
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "@${cashier.username} • Role: ${cashier.role}",
                                                fontSize = 12.sp,
                                                color = Navy600
                                            )
                                        }
                                    }

                                    // Action Buttons: Edit Name & Status Switch
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Edit Name Button
                                        IconButton(
                                            onClick = {
                                                userToEditName = cashier
                                                showEditNameDialog = true
                                            },
                                            modifier = Modifier.testTag("edit_name_button_${cashier.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Cashier Name",
                                                tint = Navy700
                                            )
                                        }

                                        // Delete Cashier Button (if more than 1 user)
                                        if (users.size > 1 && isAdmin) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteUser(cashier.id)
                                                    bannerMessage = "Cashier '${cashier.fullName.ifBlank { cashier.username }}' removed."
                                                },
                                                modifier = Modifier.testTag("delete_cashier_button_${cashier.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Cashier",
                                                    tint = Rose600
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Slate100)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Bottom Row: Toggle Switch for Active/Inactive and Assign to POS button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Toggle Switch with status label
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Switch(
                                            checked = cashier.isActive,
                                            onCheckedChange = {
                                                viewModel.toggleUserStatus(cashier) {
                                                    bannerMessage = "Status for '${cashier.fullName.ifBlank { cashier.username }}' set to ${if (!cashier.isActive) "Active" else "Inactive"}."
                                                }
                                            },
                                            modifier = Modifier.testTag("toggle_status_switch_${cashier.id}")
                                        )
                                        Column {
                                            Text(
                                                text = if (cashier.isActive) "Active Cashier" else "Inactive",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = if (cashier.isActive) Emerald700 else Rose600
                                            )
                                            Text(
                                                text = if (cashier.isActive) "Can log in & ring sales" else "Terminal access disabled",
                                                fontSize = 10.sp,
                                                color = Slate400
                                            )
                                        }
                                    }

                                    // Set as Active POS Operator Button
                                    Button(
                                        onClick = {
                                            val displayName = cashier.fullName.ifBlank { cashier.username }
                                            viewModel.setActiveCashierName(displayName, cashier)
                                            bannerMessage = "POS counter assigned to '$displayName'."
                                        },
                                        enabled = cashier.isActive && !isPosActive,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isPosActive) Emerald600 else Navy900,
                                            disabledContainerColor = if (isPosActive) Emerald100 else Slate200,
                                            disabledContentColor = if (isPosActive) Emerald700 else Slate400
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("assign_pos_button_${cashier.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isPosActive) Icons.Default.Check else Icons.Default.HowToReg,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isPosActive) "Current POS" else "Assign POS",
                                            fontSize = 11.sp,
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
    }

    // Edit Cashier Name & Profile Dialog
    if (showEditNameDialog && userToEditName != null) {
        val targetUser = userToEditName!!
        var editedFullName by remember { mutableStateOf(targetUser.fullName) }
        var editedUsername by remember { mutableStateOf(targetUser.username) }
        var editedPin by remember { mutableStateOf(targetUser.pinHash) }
        var editedRole by remember { mutableStateOf(targetUser.role) }
        var editedIsActive by remember { mutableStateOf(targetUser.isActive) }
        var validationError by remember { mutableStateOf<String?>(null) }

        val roles = listOf("CASHIER", "ADMIN", "SUPER_ADMIN", "MANAGER", "EMPLOYEE")

        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Navy900)
                    Text(
                        text = "Edit Cashier Details",
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Update cashier full name, username, and counter authorization.",
                        fontSize = 12.sp,
                        color = Navy600
                    )

                    OutlinedTextField(
                        value = editedFullName,
                        onValueChange = {
                            editedFullName = it
                            validationError = null
                        },
                        label = { Text("Cashier Full Name *") },
                        placeholder = { Text("e.g. Muhammad Umer") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_cashier_name_input")
                    )

                    OutlinedTextField(
                        value = editedUsername,
                        onValueChange = {
                            editedUsername = it
                            validationError = null
                        },
                        label = { Text("Login Username *") },
                        placeholder = { Text("e.g. umer_pos") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_cashier_username_input")
                    )

                    OutlinedTextField(
                        value = editedPin,
                        onValueChange = {
                            editedPin = it
                            validationError = null
                        },
                        label = { Text("Terminal PIN / Password *") },
                        placeholder = { Text("Minimum 4 characters") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_cashier_pin_input")
                    )

                    Text("Assigned Role:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Navy800)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        roles.forEach { r ->
                            FilterChip(
                                selected = editedRole.equals(r, ignoreCase = true),
                                onClick = { editedRole = r },
                                label = { Text(r, fontSize = 10.sp) },
                                modifier = Modifier.testTag("edit_role_chip_$r")
                            )
                        }
                    }

                    // Active / Inactive Status Row in Dialog
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
                                Text("Cashier Status", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy900)
                                Text(
                                    text = if (editedIsActive) "Active (Enabled)" else "Inactive (Disabled)",
                                    fontSize = 11.sp,
                                    color = if (editedIsActive) Emerald700 else Rose600
                                )
                            }
                            Switch(
                                checked = editedIsActive,
                                onCheckedChange = { editedIsActive = it },
                                modifier = Modifier.testTag("edit_cashier_status_switch")
                            )
                        }
                    }

                    if (validationError != null) {
                        Text(text = validationError!!, color = Rose600, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedFullName.isBlank()) {
                            validationError = "Cashier Full Name cannot be empty."
                        } else if (editedUsername.isBlank()) {
                            validationError = "Username is required."
                        } else if (editedPin.isBlank()) {
                            validationError = "Terminal PIN is required."
                        } else {
                            val updatedUser = targetUser.copy(
                                fullName = editedFullName.trim(),
                                username = editedUsername.trim(),
                                pinHash = editedPin.trim(),
                                role = editedRole,
                                isActive = editedIsActive
                            )
                            viewModel.saveUser(updatedUser) {
                                val savedName = editedFullName.trim()
                                if (activeCashierName.equals(targetUser.fullName, ignoreCase = true) ||
                                    activeCashierName.equals(targetUser.username, ignoreCase = true)) {
                                    if (editedIsActive) {
                                        viewModel.setActiveCashierName(savedName, updatedUser)
                                    }
                                }
                                bannerMessage = "Cashier name updated to '$savedName'."
                                showEditNameDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("save_cashier_name_button")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditNameDialog = false },
                    modifier = Modifier.testTag("cancel_edit_cashier_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add New Cashier Dialog
    if (showAddCashierDialog) {
        var newFullName by remember { mutableStateOf("") }
        var newUsername by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var newRole by remember { mutableStateOf("CASHIER") }
        var newIsActive by remember { mutableStateOf(true) }
        var setAsPosOperator by remember { mutableStateOf(true) }
        var addError by remember { mutableStateOf<String?>(null) }

        val roles = listOf("CASHIER", "ADMIN", "SUPER_ADMIN", "MANAGER", "EMPLOYEE")

        AlertDialog(
            onDismissRequest = { showAddCashierDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Navy900)
                    Text(
                        text = "Add New Cashier",
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Register a new cashier operator with terminal access.",
                        fontSize = 12.sp,
                        color = Navy600
                    )

                    OutlinedTextField(
                        value = newFullName,
                        onValueChange = {
                            newFullName = it
                            addError = null
                        },
                        label = { Text("Cashier Full Name *") },
                        placeholder = { Text("e.g. Muhammad Umer") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_cashier_fullname_input")
                    )

                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = {
                            newUsername = it
                            addError = null
                        },
                        label = { Text("Username / Terminal ID *") },
                        placeholder = { Text("e.g. umer_pos") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_cashier_username_input")
                    )

                    OutlinedTextField(
                        value = newPin,
                        onValueChange = {
                            newPin = it
                            addError = null
                        },
                        label = { Text("Terminal PIN / Password *") },
                        placeholder = { Text("Minimum 4 characters") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_cashier_pin_input")
                    )

                    Text("Role:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Navy800)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        roles.forEach { r ->
                            FilterChip(
                                selected = newRole.equals(r, ignoreCase = true),
                                onClick = { newRole = r },
                                label = { Text(r, fontSize = 10.sp) },
                                modifier = Modifier.testTag("new_role_chip_$r")
                            )
                        }
                    }

                    // Active Switch
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
                                Text("Initial Status", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy900)
                                Text(if (newIsActive) "Active" else "Inactive", fontSize = 11.sp, color = if (newIsActive) Emerald700 else Rose600)
                            }
                            Switch(
                                checked = newIsActive,
                                onCheckedChange = { newIsActive = it },
                                modifier = Modifier.testTag("new_cashier_status_switch")
                            )
                        }
                    }

                    if (newIsActive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { setAsPosOperator = !setAsPosOperator }
                        ) {
                            Checkbox(
                                checked = setAsPosOperator,
                                onCheckedChange = { setAsPosOperator = it },
                                modifier = Modifier.testTag("new_set_pos_checkbox")
                            )
                            Text("Assign as active POS counter cashier immediately", fontSize = 12.sp, color = Navy900)
                        }
                    }

                    if (addError != null) {
                        Text(text = addError!!, color = Rose600, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFullName.isBlank()) {
                            addError = "Cashier Full Name is required."
                        } else if (newUsername.isBlank()) {
                            addError = "Username is required."
                        } else if (newPin.isBlank()) {
                            addError = "Terminal PIN is required."
                        } else {
                            viewModel.addOrUpdateCashier(
                                fullName = newFullName.trim(),
                                username = newUsername.trim(),
                                pin = newPin.trim(),
                                role = newRole,
                                userId = 0L,
                                isActive = newIsActive,
                                setAsActive = setAsPosOperator && newIsActive
                            ) {
                                bannerMessage = "New cashier '${newFullName.trim()}' added successfully."
                                showAddCashierDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("create_cashier_submit_button")
                ) {
                    Text("Create Cashier")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddCashierDialog = false },
                    modifier = Modifier.testTag("cancel_add_cashier_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
