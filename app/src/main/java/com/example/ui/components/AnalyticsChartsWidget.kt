package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.viewmodel.CategorySalesPoint
import com.example.ui.viewmodel.DaySalesPoint
import com.example.ui.viewmodel.TopProductPoint

/**
 * High-performance, D3 & Recharts inspired Business Analytics component.
 * Ensures charts and analytics strictly populate ONLY after successful license verification.
 */
@Composable
fun AnalyticsChartsWidget(
    salesTrend: List<DaySalesPoint>,
    topProducts: List<TopProductPoint> = emptyList(),
    topCategories: List<CategorySalesPoint> = emptyList(),
    currencySymbol: String = "Rs.",
    isLicenseVerified: Boolean = false,
    onNavigateToActivation: () -> Unit = {},
    installationId: String = ""
) {
    // If terminal license is NOT verified, gate charts with security lock state
    if (!isLicenseVerified) {
        LockedLicenseAnalyticsCard(
            installationId = installationId,
            onNavigateToActivation = onNavigateToActivation
        )
        return
    }

    // Unlocked Commercial Analytics State
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Daily Sales Trends, 1: Top Products, 2: Categories
    var chartMetric by remember { mutableStateOf("Revenue") } // "Revenue" or "Volume"
    var selectedDayIndex by remember(salesTrend) { mutableIntStateOf((salesTrend.size - 1).coerceAtLeast(0)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_charts_unlocked"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Title + Verified Badge + Metric Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BentoPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Analytics",
                            tint = BentoPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Business Analytics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BentoCardDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Emerald500.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = Emerald500,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Licensed",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald500
                                    )
                                }
                            }
                        }
                        Text(
                            text = "D3 / Recharts-grade telemetry & trends",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Metric Switcher Pill (Revenue / Volume)
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

            // Sub-Tab Switcher: Trends, Top Products, Categories
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
                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Daily Trends", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Leaderboard, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Top Products", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                )

                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Category, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Categories", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: Daily Sales Trends (D3 / Recharts Spline & Area Graph)
                    val currentDay = salesTrend.getOrNull(selectedDayIndex) ?: salesTrend.lastOrNull()
                    val totalWeekRevenue = salesTrend.sumOf { it.totalRevenue }
                    val totalWeekVolume = salesTrend.sumOf { it.totalVolume }

                    // Recharts-style Metric Highlight Pill
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
                                text = "7-Day Total ${if (chartMetric == "Revenue") "Sales Revenue" else "Orders Count"}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = if (chartMetric == "Revenue") "$currencySymbol ${totalWeekRevenue.toInt()}" else "$totalWeekVolume Sales",
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

                    // Recharts / D3 Interactive Spline + Area Canvas Chart
                    RechartsStyleDailySalesChart(
                        salesTrend = salesTrend,
                        isRevenueMode = chartMetric == "Revenue",
                        selectedIndex = selectedDayIndex,
                        onSelectDay = { selectedDayIndex = it },
                        currencySymbol = currencySymbol
                    )
                }
                1 -> {
                    // TAB 1: Top-Performing Products
                    TopPerformingProductsChart(
                        products = topProducts,
                        currencySymbol = currencySymbol,
                        isRevenueMode = chartMetric == "Revenue"
                    )
                }
                2 -> {
                    // TAB 2: Top Categories Breakdown
                    TopCategoriesBreakdownWidget(
                        categories = topCategories,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
    }
}

/**
 * Locked Security State: Prominently displays an activation gate when the terminal is unverified.
 */
@Composable
fun LockedLicenseAnalyticsCard(
    installationId: String,
    onNavigateToActivation: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_license_locked_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Background preview graphic (D3/Recharts ghost outline)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val w = size.width
                val h = size.height
                // Light dashed grid
                for (i in 0..4) {
                    val y = (h / 4) * i
                    drawLine(
                        color = Color(0xFFE2E8F0).copy(alpha = 0.6f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                // Ghost bars
                val barCount = 7
                val bw = (w / barCount) * 0.45f
                val sp = w / barCount
                for (i in 0 until barCount) {
                    val xc = i * sp + sp / 2
                    val bh = h * (0.3f + (i % 3) * 0.2f)
                    drawRoundRect(
                        color = Color(0xFFCBD5E1).copy(alpha = 0.35f),
                        topLeft = Offset(xc - bw / 2, h - bh),
                        size = Size(bw, bh),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            // Foreground Security Shield & Activation Gate
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.5.dp, BentoAmber),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "License Locked",
                            tint = BentoAmber,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "License Verification Required",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoCardDark
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Daily sales trends and top-performing product analytics populate exclusively after successful commercial terminal license verification.",
                    fontSize = 12.5.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (installationId.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "Terminal ID: $installationId",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BentoCardDark,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                Button(
                    onClick = onNavigateToActivation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BentoPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("analytics_verify_license_btn")
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verify & Unlock License",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Recharts & D3 Styled Interactive Daily Sales Chart
 * Features:
 * - Area gradient fill under smooth cubic-bezier curve
 * - Rounded bar overlay
 * - Recharts-style dashed grid lines
 * - Interactive tap/drag scrubber showing live tooltip with value
 */
@Composable
fun RechartsStyleDailySalesChart(
    salesTrend: List<DaySalesPoint>,
    isRevenueMode: Boolean,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit,
    currencySymbol: String
) {
    val maxValue = remember(salesTrend, isRevenueMode) {
        val max = if (isRevenueMode) salesTrend.maxOfOrNull { it.totalRevenue } ?: 1.0 else salesTrend.maxOfOrNull { it.totalVolume.toDouble() } ?: 1.0
        if (max <= 0) 100.0 else max
    }

    val activePoint = salesTrend.getOrNull(selectedIndex)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Floating Recharts Tooltip Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center
        ) {
            if (activePoint != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BentoCardDark,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Gold400)
                        )
                        Text(
                            text = "${activePoint.dayName}, ${activePoint.dateLabel}:",
                            color = Slate300,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isRevenueMode) "$currencySymbol ${activePoint.totalRevenue.toInt()}" else "${activePoint.totalVolume} Orders",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Canvas Area + Line + Bar Graphic
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(salesTrend) {
                        detectTapGestures { offset ->
                            val barWidth = size.width / salesTrend.size.coerceAtLeast(1)
                            val tappedIndex = (offset.x / barWidth).toInt().coerceIn(0, salesTrend.size - 1)
                            onSelectDay(tappedIndex)
                        }
                    }
                    .pointerInput(salesTrend) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val barWidth = size.width / salesTrend.size.coerceAtLeast(1)
                            val tappedIndex = (change.position.x / barWidth).toInt().coerceIn(0, salesTrend.size - 1)
                            onSelectDay(tappedIndex)
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 24.dp.toPx()
                val count = salesTrend.size.coerceAtLeast(1)
                val spacing = canvasWidth / count
                val barWidth = spacing * 0.42f

                // 1. Draw D3 / Recharts horizontal dashed grid lines
                val gridLines = 4
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                for (i in 0..gridLines) {
                    val y = (canvasHeight / gridLines) * i
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                val points = mutableListOf<Offset>()

                // 2. Draw Bars and collect curve control points
                salesTrend.forEachIndexed { index, point ->
                    val value = if (isRevenueMode) point.totalRevenue else point.totalVolume.toDouble()
                    val ratio = (value / maxValue).toFloat().coerceIn(0.06f, 1f)
                    val barHeight = canvasHeight * ratio

                    val xCenter = (index * spacing) + (spacing / 2)
                    val xLeft = xCenter - (barWidth / 2)
                    val yTop = canvasHeight - barHeight

                    val isSelected = index == selectedIndex

                    // Recharts soft bar fill
                    val barColor = if (isSelected) {
                        if (isRevenueMode) BentoPrimary.copy(alpha = 0.85f) else BentoIndigo.copy(alpha = 0.85f)
                    } else {
                        if (isRevenueMode) BentoPrimaryLight else BentoIndigoLight
                    }

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(xLeft, yTop),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Vertical cursor guide line for active point (Recharts Cursor)
                    if (isSelected) {
                        drawLine(
                            color = BentoPrimary.copy(alpha = 0.5f),
                            start = Offset(xCenter, 0f),
                            end = Offset(xCenter, canvasHeight),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    points.add(Offset(xCenter, yTop))
                }

                // 3. Draw Spline Area Gradient (Recharts <Area fill="url(#gradient)" />)
                if (points.isNotEmpty()) {
                    val areaPath = Path()
                    areaPath.moveTo(points.first().x, canvasHeight)
                    areaPath.lineTo(points.first().x, points.first().y)

                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val c1 = Offset(prev.x + (curr.x - prev.x) / 2, prev.y)
                        val c2 = Offset(prev.x + (curr.x - prev.x) / 2, curr.y)
                        areaPath.cubicTo(c1.x, c1.y, c2.x, c2.y, curr.x, curr.y)
                    }

                    areaPath.lineTo(points.last().x, canvasHeight)
                    areaPath.close()

                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                BentoPrimary.copy(alpha = 0.35f),
                                BentoPrimary.copy(alpha = 0.02f)
                            ),
                            startY = 0f,
                            endY = canvasHeight
                        )
                    )

                    // 4. Draw Line Stroke on top of area
                    val linePath = Path()
                    linePath.moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val c1 = Offset(prev.x + (curr.x - prev.x) / 2, prev.y)
                        val c2 = Offset(prev.x + (curr.x - prev.x) / 2, curr.y)
                        linePath.cubicTo(c1.x, c1.y, c2.x, c2.y, curr.x, curr.y)
                    }

                    drawPath(
                        path = linePath,
                        color = BentoPrimary,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 5. Draw point dots with active indicator ring
                    points.forEachIndexed { idx, pt ->
                        val isSelected = idx == selectedIndex
                        if (isSelected) {
                            // Outer glow ring
                            drawCircle(
                                color = BentoPrimary.copy(alpha = 0.25f),
                                radius = 10.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = BentoPrimaryDark,
                                radius = 6.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.5.dp.toPx(),
                                center = pt
                            )
                        } else {
                            drawCircle(
                                color = Color.White,
                                radius = 4.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = BentoPrimary,
                                radius = 2.5.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            }
        }

        // X-Axis Day & Date Labels
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
                        color = if (isSelected) BentoPrimaryDark else Color.LightGray
                    )
                }
            }
        }
    }
}

/**
 * Recharts & D3 Styled Top-Performing Products Visualization
 */
@Composable
fun TopPerformingProductsChart(
    products: List<TopProductPoint>,
    currencySymbol: String,
    isRevenueMode: Boolean
) {
    if (products.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF8FAFC)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = Slate300, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No product sales recorded yet.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Completed POS transactions will rank top items here!",
                    fontSize = 11.sp,
                    color = Slate400
                )
            }
        }
    } else {
        val maxMetricValue = remember(products, isRevenueMode) {
            val max = if (isRevenueMode) {
                products.maxOfOrNull { it.totalRevenue } ?: 1.0
            } else {
                products.maxOfOrNull { it.totalUnitsSold } ?: 1.0
            }
            if (max <= 0) 1.0 else max
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top ${products.size} Performing Items",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
                Text(
                    text = if (isRevenueMode) "Ranked by Sales Revenue" else "Ranked by Units Sold",
                    fontSize = 11.sp,
                    color = BentoPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Products Ranking List with Recharts-style horizontal bars
            products.forEachIndexed { index, product ->
                val rank = index + 1
                val rankBadgeColor = when (rank) {
                    1 -> Gold500
                    2 -> Color(0xFF94A3B8)
                    3 -> Color(0xFFD97706)
                    else -> Slate400
                }

                val currentVal = if (isRevenueMode) product.totalRevenue else product.totalUnitsSold
                val fillProgress = (currentVal / maxMetricValue).toFloat().coerceIn(0.08f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = fillProgress,
                    animationSpec = tween(durationMillis = 500)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Title + Rank + Category + Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                // Rank Badge
                                Surface(
                                    shape = CircleShape,
                                    color = rankBadgeColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "#$rank",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = rankBadgeColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = product.productName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoCardDark
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Slate200
                                        ) {
                                            Text(
                                                text = product.category,
                                                fontSize = 9.5.sp,
                                                color = Slate700,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }

                                        Text(
                                            text = "${product.totalUnitsSold.toInt()} sold • Stock: ${product.currentStock.toInt()}",
                                            fontSize = 10.sp,
                                            color = if (product.currentStock <= 5) BentoRose else Color.Gray
                                        )
                                    }
                                }
                            }

                            // Revenue & Share %
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$currencySymbol ${product.totalRevenue.toInt()}",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BentoPrimaryDark
                                )
                                Text(
                                    text = "${product.percentage.toInt()}% share",
                                    fontSize = 10.sp,
                                    color = BentoPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Recharts-style Gradient Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(BentoPrimary, BentoEmerald)
                                        )
                                    )
                            )
                        }
                    }
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
    val categoryColors: List<Color> = listOf(
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
            // Stacked Bar Visualizing Category Distribution (Recharts Segmented Bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                categories.forEachIndexed { index, cat ->
                    val color = categoryColors.getOrElse(index) { BentoPrimary }
                    val weight = cat.percentage.toFloat().coerceAtLeast(0.02f)
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
