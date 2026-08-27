package com.example.ui.screens

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

@Composable
fun UserManagementScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val users by viewModel.users.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val activeCashierName by viewModel.activeCashierName.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<User?>(null) }

    val isAdmin = remember(activeUser) {
        val role = activeUser?.role?.uppercase() ?: "SUPER_ADMIN"
        role in listOf("SUPER_ADMIN", "ADMIN", "SUPER ADMIN", "ADMINISTRATOR") || activeUser == null
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Staff & Cashier Management",
                subtitle = "${users.size} Operators • Active Cashier: $activeCashierName",
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
            // Active Cashier Banner
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
                                imageVector = Icons.Default.Badge,
                                contentDescription = "Cashier Badge",
                                tint = Navy900,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text("Current Active Cashier", fontSize = 11.sp, color = Slate300)
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
                        backgroundColor = Gold100,
                        textColor = Gold600
                    )
                }
            }

            if (!isAdmin) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Amber100),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Amber800, modifier = Modifier.size(16.dp))
                        Text(
                            text = "You are in Standard Operator mode. Cashier selection is enabled, while profile editing requires Admin role.",
                            fontSize = 11.sp,
                            color = Amber800
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(users, key = { it.id }) { user ->
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
                                                text = "Active Cashier",
                                                backgroundColor = Emerald100,
                                                textColor = Emerald700
                                            )
                                        }
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
                                        IconButton(
                                            onClick = {
                                                selectedUserForEdit = user
                                                showAddEditDialog = true
                                            },
                                            modifier = Modifier.testTag("edit_user_${user.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Navy600)
                                        }

                                        if (users.size > 1) {
                                            IconButton(
                                                onClick = { viewModel.deleteUser(user.id) },
                                                modifier = Modifier.testTag("delete_user_${user.id}")
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Rose600)
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
                                    text = if (isCashierActive) "Currently assigned to all new sales" else "Click to assign as current cashier",
                                    fontSize = 11.sp,
                                    color = if (isCashierActive) Emerald700 else Slate400
                                )

                                Button(
                                    onClick = {
                                        val targetName = user.fullName.ifBlank { user.username }
                                        viewModel.setActiveCashierName(targetName, user)
                                    },
                                    enabled = !isCashierActive,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCashierActive) Emerald600 else Navy900,
                                        disabledContainerColor = Emerald100,
                                        disabledContentColor = Emerald700
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
                                        text = if (isCashierActive) "Selected" else "Set as Active",
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

        if (showAddEditDialog) {
            var fullName by remember { mutableStateOf(selectedUserForEdit?.fullName ?: "") }
            var username by remember { mutableStateOf(selectedUserForEdit?.username ?: "") }
            var pin by remember { mutableStateOf(selectedUserForEdit?.pinHash ?: "") }
            var role by remember { mutableStateOf(selectedUserForEdit?.role ?: "CASHIER") }
            var errorMsg by remember { mutableStateOf<String?>(null) }

            val roles = listOf("SUPER_ADMIN", "ADMIN", "CASHIER", "EMPLOYEE")

            AlertDialog(
                onDismissRequest = { showAddEditDialog = false },
                title = {
                    Text(
                        text = if (selectedUserForEdit == null) "Add Staff Member" else "Edit User Profile",
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
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("user_fullname_input")
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("user_username_input")
                        )

                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = { Text("Terminal PIN / Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("user_pin_input")
                        )

                        Text("Assigned Role & Permissions:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy900)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            roles.forEach { r ->
                                FilterChip(
                                    selected = role == r,
                                    onClick = { role = r },
                                    label = { Text(r, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("role_chip_$r")
                                )
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
                            if (username.isBlank() || pin.isBlank()) {
                                errorMsg = "Username and PIN are required."
                            } else {
                                val userToSave = (selectedUserForEdit ?: User(
                                    username = username.trim(),
                                    pinHash = pin.trim()
                                )).copy(
                                    fullName = fullName.trim(),
                                    username = username.trim(),
                                    pinHash = pin.trim(),
                                    role = role
                                )
                                viewModel.saveUser(userToSave) {
                                    if (selectedUserForEdit == null || activeCashierName.equals(selectedUserForEdit?.fullName, ignoreCase = true)) {
                                        val assignedName = fullName.trim().ifBlank { username.trim() }
                                        viewModel.setActiveCashierName(assignedName, userToSave)
                                    }
                                    showAddEditDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        modifier = Modifier.testTag("save_user_button")
                    ) {
                        Text("Save Cashier / Staff")
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

