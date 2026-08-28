package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    fun formatDate(timestamp: Long, pattern: String = "dd-MMM-yyyy"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long, is24Hour: Boolean = false, includeSeconds: Boolean = true): String {
        val pattern = when {
            is24Hour && includeSeconds -> "HH:mm:ss"
            is24Hour && !includeSeconds -> "HH:mm"
            !is24Hour && includeSeconds -> "hh:mm:ss a"
            else -> "hh:mm a"
        }
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatFullDateTime(timestamp: Long, is24Hour: Boolean = false): String {
        return "${formatDate(timestamp)} | ${formatTime(timestamp, is24Hour, includeSeconds = true)}"
    }
}

@Composable
fun LiveClockBadge(
    modifier: Modifier = Modifier,
    is24Hour: Boolean = false,
    showSeconds: Boolean = true,
    compact: Boolean = false,
    containerColor: Color = Color(0xFF0F172A).copy(alpha = 0.85f),
    borderColor: Color = Gold500.copy(alpha = 0.4f)
) {
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot_alpha"
    )

    val dateStr = remember(currentTimeMillis / 60000) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        sdf.format(Date(currentTimeMillis))
    }

    val timeStr = remember(currentTimeMillis) {
        val pattern = if (is24Hour) {
            if (showSeconds) "HH:mm:ss" else "HH:mm"
        } else {
            if (showSeconds) "hh:mm:ss a" else "hh:mm a"
        }
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.format(Date(currentTimeMillis))
    }

    Surface(
        modifier = modifier.testTag("live_clock_badge"),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 3.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Pulsing Live Dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Emerald400.copy(alpha = pulseAlpha))
            )

            // LIVE text
            Text(
                text = "LIVE",
                color = Gold400,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Text(
                text = "•",
                color = Slate400,
                fontSize = 9.sp
            )

            Text(
                text = dateStr,
                color = Color.White,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "|",
                color = Gold500.copy(alpha = 0.6f),
                fontSize = if (compact) 9.sp else 10.sp
            )

            Text(
                text = timeStr,
                color = Gold400,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun LiveClockHeaderWidget(
    modifier: Modifier = Modifier,
    is24Hour: Boolean = false,
    showSeconds: Boolean = true
) {
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_header")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    val dateStr = remember(currentTimeMillis / 60000) {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        sdf.format(Date(currentTimeMillis))
    }

    val timeStr = remember(currentTimeMillis, is24Hour, showSeconds) {
        val pattern = when {
            is24Hour && showSeconds -> "HH:mm:ss"
            is24Hour && !showSeconds -> "HH:mm"
            !is24Hour && showSeconds -> "hh:mm:ss a"
            else -> "hh:mm a"
        }
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.format(Date(currentTimeMillis))
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.9f))
            .border(1.dp, Gold500.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("live_clock_header_widget"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Emerald400.copy(alpha = pulseAlpha))
        )
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "LIVE",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Emerald400
                )
                Text(
                    text = timeStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Gold400
                )
            }
            Text(
                text = dateStr,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Slate300
            )
        }
    }
}
