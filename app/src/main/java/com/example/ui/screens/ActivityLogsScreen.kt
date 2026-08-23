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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActivityLogsScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val logs by viewModel.activityLogs.collectAsState()

    Scaffold(
        topBar = {
            AppHeader(
                title = "Audit & Activity Logs",
                subtitle = "${logs.size} System Events Recorded",
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
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No activity logs recorded yet.", color = Navy500)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
                        val formattedDate = dateFormat.format(Date(log.timestamp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(log.action, fontWeight = FontWeight.Bold, color = Navy900, fontSize = 13.sp)
                                    Text(log.module, color = Blue600, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text(log.details, fontSize = 12.sp, color = Navy700)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("By ${log.performedBy} on $formattedDate", fontSize = 10.sp, color = Navy500)
                            }
                        }
                    }
                }
            }
        }
    }
}
