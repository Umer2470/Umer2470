package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BentoAmber
import com.example.ui.theme.BentoCardDark
import com.example.ui.theme.BentoEmerald
import com.example.ui.theme.BentoEmeraldLight
import com.example.ui.theme.BentoIndigo
import com.example.ui.theme.BentoIndigoLight
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoPrimaryLight
import com.example.ui.theme.BentoPurple
import com.example.ui.theme.BentoRose
import com.example.ui.viewmodel.CategorySalesPoint
import com.example.ui.viewmodel.DaySalesPoint

@Composable
fun AnalyticsChartsWidget(
    salesTrend: List<DaySalesPoint>,
    topCategories: List<CategorySalesPoint>,
    currencySymbol: String = "Rs."
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 7-Day Trend, 1: Top Categories
    var chartMetric by remember { mutableStateOf("Revenue") } // "Revenue" or "Volume"
    var selectedDayIndex by remember(salesTrend) { mutableIntStateOf((salesTrend.size - 1).coerceAtLeast(0)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BentoPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Analytics",
                            tint = BentoPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Sales & Inventory Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BentoCardDark
                        )
                        Text(
                            text = "Visual performance dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Metric Switcher Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BentoPrimaryLight
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (chartMetric == "Revenue") BentoPrimary else Color.Transparent)
                                .clickable { chartMetric = "Revenue" }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Revenue",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (chartMetric == "Revenue") Color.White else BentoPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (chartMetric == "Volume") BentoPrimary else Color.Transparent)
                                .clickable { chartMetric = "Volume" }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Volume",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (chartMetric == "Volume") Color.White else BentoPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-Tab Switcher
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFF1F5F9),
                contentColor = BentoPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .padding(2.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("7-Day Sales Trend", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Top Categories", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab 0: 7-Day Trend Bar & Spline Chart
            if (selectedTab == 0) {
                val currentDay = salesTrend.getOrNull(selectedDayIndex) ?: salesTrend.lastOrNull()
                val totalWeekRevenue = salesTrend.sumOf { it.totalRevenue }
                val totalWeekVolume = salesTrend.sumOf { it.totalVolume }

                // Highlights Summary Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "7-Day Total ${if (chartMetric == "Revenue") "Revenue" else "Volume"}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = if (chartMetric == "Revenue") "$currencySymbol ${totalWeekRevenue.toInt()}" else "$totalWeekVolume Invoices",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoPrimaryDark
                        )
                    }

                    if (currentDay != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BentoEmeraldLight
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "${currentDay.dayName} (${currentDay.dateLabel})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoEmerald
                                )
                                Text(
                                    text = "$currencySymbol ${currentDay.totalRevenue.toInt()} • ${currentDay.totalVolume} Sales",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoEmerald
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Native Canvas Chart
                SevenDaySalesCanvasChart(
                    salesTrend = salesTrend,
                    isRevenueMode = chartMetric == "Revenue",
                    selectedIndex = selectedDayIndex,
                    onSelectDay = { selectedDayIndex = it }
                )
            } else {
                // Tab 1: Top Categories Breakdown
                TopCategoriesBreakdownWidget(
                    categories = topCategories,
                    currencySymbol = currencySymbol
                )
            }
        }
    }
}

/**
 * Native Canvas Bar & Trend Chart Component
 */
@Composable
fun SevenDaySalesCanvasChart(
    salesTrend: List<DaySalesPoint>,
    isRevenueMode: Boolean,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit
) {
    val maxValue = remember(salesTrend, isRevenueMode) {
        val max = if (isRevenueMode) salesTrend.maxOfOrNull { it.totalRevenue } ?: 1.0 else salesTrend.maxOfOrNull { it.totalVolume.toDouble() } ?: 1.0
        if (max <= 0) 100.0 else max
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .pointerInput(salesTrend, isRevenueMode) {
                        detectTapGestures { offset ->
                            val barWidth = size.width / salesTrend.size.coerceAtLeast(1)
                            val tappedIndex = (offset.x / barWidth).toInt().coerceIn(0, salesTrend.size - 1)
                            onSelectDay(tappedIndex)
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 30.dp.toPx() // leave space for bottom labels
                val count = salesTrend.size.coerceAtLeast(1)
                val barWidth = (canvasWidth / count) * 0.45f
                val spacing = canvasWidth / count

                // Draw Horizontal Grid Lines
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = (canvasHeight / gridLines) * i
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val linePath = Path()
                val points = mutableListOf<Offset>()

                salesTrend.forEachIndexed { index, point ->
                    val value = if (isRevenueMode) point.totalRevenue else point.totalVolume.toDouble()
                    val ratio = (value / maxValue).toFloat().coerceIn(0.05f, 1f)
                    val barHeight = canvasHeight * ratio

                    val xCenter = (index * spacing) + (spacing / 2)
                    val xLeft = xCenter - (barWidth / 2)
                    val yTop = canvasHeight - barHeight

                    val isSelected = index == selectedIndex

                    // Draw Bar Background / Active Fill
                    val barColor = if (isSelected) {
                        if (isRevenueMode) BentoPrimary else BentoIndigo
                    } else {
                        if (isRevenueMode) BentoPrimaryLight else BentoIndigoLight
                    }

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(xLeft, yTop),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )

                    // Active border glow for selected index
                    if (isSelected) {
                        drawRoundRect(
                            color = BentoPrimaryDark,
                            topLeft = Offset(xLeft - 1.dp.toPx(), yTop - 1.dp.toPx()),
                            size = Size(barWidth + 2.dp.toPx(), barHeight + 2.dp.toPx()),
                            cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    val pointOffset = Offset(xCenter, yTop)
                    points.add(pointOffset)

                    if (index == 0) {
                        linePath.moveTo(pointOffset.x, pointOffset.y)
                    } else {
                        val prevPoint = points[index - 1]
                        val controlPoint1 = Offset(prevPoint.x + (pointOffset.x - prevPoint.x) / 2, prevPoint.y)
                        val controlPoint2 = Offset(prevPoint.x + (pointOffset.x - prevPoint.x) / 2, pointOffset.y)
                        linePath.cubicTo(
                            controlPoint1.x, controlPoint1.y,
                            controlPoint2.x, controlPoint2.y,
                            pointOffset.x, pointOffset.y
                        )
                    }
                }

                // Draw Spline Overlay Line
                if (points.isNotEmpty()) {
                    drawPath(
                        path = linePath,
                        color = BentoPrimary,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    points.forEachIndexed { idx, pt ->
                        val isSelected = idx == selectedIndex
                        drawCircle(
                            color = if (isSelected) BentoPrimaryDark else Color.White,
                            radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = BentoPrimary,
                            radius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }

        // Days Label Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            salesTrend.forEachIndexed { index, point ->
                val isSelected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectDay(index) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = point.dayName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) BentoPrimary else Color.Gray
                    )
                    Text(
                        text = point.dateLabel.take(2),
                        fontSize = 9.sp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

/**
 * Top Categories Breakdown Visual Component
 */
@Composable
fun TopCategoriesBreakdownWidget(
    categories: List<CategorySalesPoint>,
    currencySymbol: String
) {
    val categoryColors = listOf(
        BentoPrimary, BentoEmerald, BentoIndigo, BentoAmber, BentoPurple, BentoRose
    )

    if (categories.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF8FAFC)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No category sales recorded yet.\nNew sales will automatically populate category charts!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Stacked Bar Visualizing Category Distribution
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                categories.forEachIndexed { index, cat ->
                    val color = categoryColors.getOrElse(index) { BentoPrimary }
                    val weight = cat.percentage.coerceAtLeast(2f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // List of Categories
            categories.forEachIndexed { index, cat ->
                val color = categoryColors.getOrElse(index) { BentoPrimary }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = cat.categoryName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoCardDark
                            )
                            Text(
                                text = "${cat.totalUnitsSold.toInt()} items sold",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$currencySymbol ${cat.totalAmount.toInt()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimaryDark
                        )
                        Text(
                            text = "${cat.percentage.toInt()}% of total",
                            fontSize = 10.sp,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
