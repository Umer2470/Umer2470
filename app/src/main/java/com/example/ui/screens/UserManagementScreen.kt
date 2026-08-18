package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.UserAccount
import com.example.ui.viewmodel.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(viewModel: StoreViewModel) {
    val allUsers by viewModel.allUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allStores by viewModel.allStores.collectAsState()

    var showAddUserDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<UserAccount?>(null) }
    var userToDelete by remember { mutableStateOf<UserAccount?>(null) }

    // Dialog state
    var nameInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var pinCodeInput by remember { mutableStateOf("") }
    var roleInput by remember { mutableStateOf("EMPLOYEE") }
    var assignedStoreIdInput by remember { mutableStateOf(1L) }
    var selectedStoreIdsList by remember { mutableStateOf<List<Long>>(listOf(1L)) }
    var adminPinVerificationInput by remember { mutableStateOf("") }

    var expandedRoleDropdown by remember { mutableStateOf(false) }
    var expandedStoreDropdown by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2537)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "Staff & User Roles Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Super Admin Role Control • Total Staff: ${allUsers.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (allUsers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No user accounts found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(allUsers) { user ->
                        val assignedIds = user.getAssignedStoreIdsList()
                        val assignedNames = if (user.role == "SUPER_ADMIN") {
                            "All Store Branches (Super Admin)"
                        } else {
                            val names = allStores.filter { it.id in assignedIds }.map { it.storeName }
                            if (names.isNotEmpty()) names.joinToString(", ") else "Store Branch #1"
                        }

                        UserCardItem(
                            user = user,
                            assignedStoreName = assignedNames,
                            onEdit = {
                                selectedUserForEdit = user
                                nameInput = user.name
                                usernameInput = user.username
                                phoneInput = user.phone
                                pinCodeInput = user.pinCode
                                roleInput = user.role
                                assignedStoreIdInput = user.assignedStoreId
                                selectedStoreIdsList = user.getAssignedStoreIdsList()
                                adminPinVerificationInput = ""
                            },
                            onDelete = {
                                userToDelete = user
                                adminPinVerificationInput = ""
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                selectedUserForEdit = null
                nameInput = ""
                usernameInput = ""
                phoneInput = ""
                pinCodeInput = ""
                roleInput = "EMPLOYEE"
                adminPinVerificationInput = ""
                showAddUserDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color(0xFF0F2537),
            contentColor = Color(0xFFFFD700)
        ) {
            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add User")
        }
    }

    // ADD / EDIT USER DIALOG
    if (showAddUserDialog || selectedUserForEdit != null) {
        val isEdit = selectedUserForEdit != null
        AlertDialog(
            onDismissRequest = {
                showAddUserDialog = false
                selectedUserForEdit = null
            },
            title = {
                Text(
                    text = if (isEdit) "Edit User Account (${selectedUserForEdit?.role})" else "Create New Staff Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = pinCodeInput,
                        onValueChange = { if (it.length <= 4) pinCodeInput = it },
                        label = { Text("4-Digit Security PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedRoleDropdown,
                        onExpandedChange = { expandedRoleDropdown = !expandedRoleDropdown }
                    ) {
                        OutlinedTextField(
                            value = roleInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assign User Role") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRoleDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedRoleDropdown,
                            onDismissRequest = { expandedRoleDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Super Admin (Full Access)") },
                                onClick = {
                                    roleInput = "SUPER_ADMIN"
                                    expandedRoleDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Admin (Manager Access)") },
                                onClick = {
                                    roleInput = "ADMIN"
                                    expandedRoleDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Employee (Sales & POS Only)") },
                                onClick = {
                                    roleInput = "EMPLOYEE"
                                    expandedRoleDropdown = false
                                }
                            )
                        }
                    }

                    if (roleInput == "SUPER_ADMIN") {
                        Text(
                            text = "✓ Super Admin has full access to all store branches.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else if (allStores.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Assigned Store Branches (Select 1 or More):",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            allStores.forEach { store ->
                                val isChecked = selectedStoreIdsList.contains(store.id)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                        .clickable {
                                            selectedStoreIdsList = if (isChecked) {
                                                if (selectedStoreIdsList.size > 1) selectedStoreIdsList - store.id else selectedStoreIdsList
                                            } else {
                                                selectedStoreIdsList + store.id
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedStoreIdsList = if (checked) {
                                                selectedStoreIdsList + store.id
                                            } else {
                                                if (selectedStoreIdsList.size > 1) selectedStoreIdsList - store.id else selectedStoreIdsList
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(text = store.storeName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        if (store.address.isNotBlank()) {
                                            Text(text = store.address, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = adminPinVerificationInput,
                        onValueChange = { adminPinVerificationInput = it },
                        label = { Text("Enter Super Admin PIN to Confirm") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val csvStores = if (roleInput == "SUPER_ADMIN") "" else selectedStoreIdsList.joinToString(",")
                        val primaryStoreId = selectedStoreIdsList.firstOrNull() ?: 1L
                        val userObj = UserAccount(
                            id = selectedUserForEdit?.id ?: 0L,
                            username = usernameInput,
                            name = nameInput,
                            pinCode = pinCodeInput,
                            role = roleInput,
                            phone = phoneInput,
                            assignedStoreId = primaryStoreId,
                            assignedStoreIdsCsv = csvStores
                        )
                        viewModel.saveUser(
                            user = userObj,
                            adminPin = adminPinVerificationInput,
                            onComplete = { success, msg ->
                                viewModel.showToast(msg)
                                if (success) {
                                    showAddUserDialog = false
                                    selectedUserForEdit = null
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2537))
                ) {
                    Text("Save User Account", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddUserDialog = false
                    selectedUserForEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DELETE CONFIRMATION DIALOG
    if (userToDelete != null) {
        val user = userToDelete!!
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Confirm User Deletion", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to delete user account '${user.name}'?")
                    Text("Role: ${user.role}", fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = adminPinVerificationInput,
                        onValueChange = { adminPinVerificationInput = it },
                        label = { Text("Enter Super Admin Security PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(
                            user = user,
                            adminPin = adminPinVerificationInput,
                            onComplete = { success, msg ->
                                viewModel.showToast(msg)
                                if (success) userToDelete = null
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete Account", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UserCardItem(
    user: UserAccount,
    assignedStoreName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val roleColor = when (user.role) {
        "SUPER_ADMIN" -> Color(0xFFB45309)
        "ADMIN" -> Color(0xFF1D4ED8)
        else -> Color(0xFF15803D)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(roleColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = user.role,
                            color = roleColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Username: ${user.username} | Phone: ${user.phone.ifBlank { "N/A" }}", fontSize = 12.sp, color = Color.Gray)
                Text("Assigned Branch: $assignedStoreName", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Text("Security PIN: •••• (4-digit)", fontSize = 11.sp, color = Color.DarkGray)
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit User", tint = MaterialTheme.colorScheme.primary)
                }
                if (user.role != "SUPER_ADMIN") {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete User", tint = Color(0xFFDC2626))
                    }
                }
            }
        }
    }
}
