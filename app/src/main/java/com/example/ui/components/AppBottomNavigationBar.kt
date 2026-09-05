package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.navigation.Screen
import com.example.ui.theme.*

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val badgeCount: Int = 0
)

@Composable
fun AppBottomNavigationBar(
    currentRoute: String?,
    cartItemCount: Int = 0,
    userRole: UserRole = UserRole.SUPER_ADMIN,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember(cartItemCount, userRole) {
        if (userRole == UserRole.CASHIER) {
            listOf(
                BottomNavItem(
                    route = Screen.Pos.route,
                    label = "POS",
                    selectedIcon = Icons.Filled.PointOfSale,
                    unselectedIcon = Icons.Outlined.PointOfSale,
                    testTag = "bottom_nav_pos",
                    badgeCount = cartItemCount
                ),
                BottomNavItem(
                    route = Screen.Invoice.route,
                    label = "Invoice",
                    selectedIcon = Icons.Filled.ReceiptLong,
                    unselectedIcon = Icons.Outlined.ReceiptLong,
                    testTag = "bottom_nav_invoice"
                ),
                BottomNavItem(
                    route = Screen.Attendance.route,
                    label = "Attendance",
                    selectedIcon = Icons.Filled.Badge,
                    unselectedIcon = Icons.Outlined.Badge,
                    testTag = "bottom_nav_attendance"
                ),
                BottomNavItem(
                    route = Screen.Customers.route,
                    label = "Customers",
                    selectedIcon = Icons.Filled.People,
                    unselectedIcon = Icons.Outlined.People,
                    testTag = "bottom_nav_customers"
                )
            )
        } else {
            listOf(
                BottomNavItem(
                    route = Screen.Dashboard.route,
                    label = "Dashboard",
                    selectedIcon = Icons.Filled.Dashboard,
                    unselectedIcon = Icons.Outlined.Dashboard,
                    testTag = "bottom_nav_dashboard"
                ),
                BottomNavItem(
                    route = Screen.Pos.route,
                    label = "POS",
                    selectedIcon = Icons.Filled.PointOfSale,
                    unselectedIcon = Icons.Outlined.PointOfSale,
                    testTag = "bottom_nav_pos",
                    badgeCount = cartItemCount
                ),
                BottomNavItem(
                    route = Screen.Inventory.route,
                    label = "Products",
                    selectedIcon = Icons.Filled.Inventory2,
                    unselectedIcon = Icons.Outlined.Inventory2,
                    testTag = "bottom_nav_products"
                ),
                BottomNavItem(
                    route = Screen.Invoice.route,
                    label = "Invoice",
                    selectedIcon = Icons.Filled.ReceiptLong,
                    unselectedIcon = Icons.Outlined.ReceiptLong,
                    testTag = "bottom_nav_invoice"
                ),
                BottomNavItem(
                    route = Screen.Settings.route,
                    label = "Settings",
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    testTag = "bottom_nav_settings"
                )
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .border(
                width = 1.dp,
                color = Slate200,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route

                val indicatorBg by animateColorAsState(
                    targetValue = if (isSelected) Navy900 else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "indicator_bg"
                )

                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Navy500,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "icon_color"
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Navy900 else Navy500,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "text_color"
                )

                val pillWidth by animateDpAsState(
                    targetValue = if (isSelected) 56.dp else 44.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "pill_width"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Navy900.copy(alpha = 0.12f)),
                            onClick = { onNavigate(item.route) }
                        )
                        .testTag(item.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(pillWidth)
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(indicatorBg),
                        contentAlignment = Alignment.Center
                    ) {
                        BadgedBox(
                            badge = {
                                if (item.badgeCount > 0) {
                                    Badge(
                                        containerColor = if (isSelected) Gold500 else Emerald600,
                                        contentColor = if (isSelected) Navy900 else Color.White,
                                        modifier = Modifier.offset(x = 4.dp, y = (-2).dp)
                                    ) {
                                        Text(
                                            text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.label,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
