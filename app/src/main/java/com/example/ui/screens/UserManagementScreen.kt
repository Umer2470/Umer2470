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

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<User?>(null) }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Staff & Role Management",
                subtitle = "${users.size} Registered Operators",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedUserForEdit = null
                    showAddEditDialog = true
                },
                containerColor = Navy900,
                contentColor = Gold500,
                modifier = Modifier.testTag("add_user_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add User")
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    val isCurrent = activeUser?.id == user.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_card_${user.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = user.fullName.ifBlank { user.username },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Navy900
                                    )
                                    if (isCurrent) {
                                        StatusBadge(
                                            text = "Active Session",
                                            backgroundColor = Emerald100,
                                            textColor = Emerald600
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Username: @${user.username} • Role: ${user.role}",
                                    fontSize = 12.sp,
                                    color = Navy500
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    showAddEditDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        modifier = Modifier.testTag("save_user_button")
                    ) {
                        Text("Save User")
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

