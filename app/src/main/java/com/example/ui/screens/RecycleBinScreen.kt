package com.example.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

@Composable
fun RecycleBinScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val deletedProducts by viewModel.recycleBinProducts.collectAsState()

    Scaffold(
        topBar = {
            AppHeader(
                title = "Recycle Bin & Trash",
                subtitle = "${deletedProducts.size} Deleted Items",
                onBackClick = onNavigateBack
            )
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (deletedProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Recycle bin is empty. No deleted records found.", color = Navy500)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(deletedProducts, key = { it.id }) { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                Column {
                                    Text(product.name, fontWeight = FontWeight.Bold, color = Navy900)
                                    Text("Category: ${product.category}", fontSize = 12.sp, color = Navy500)
                                }
                                Row {
                                    IconButton(
                                        onClick = { viewModel.restoreProduct(product.id) },
                                        modifier = Modifier.testTag("restore_product_${product.id}")
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = "Restore", tint = Emerald600)
                                    }
                                    IconButton(
                                        onClick = { viewModel.hardDeleteProduct(product.id) }
                                    ) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = "Permanent Delete", tint = Rose600)
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
