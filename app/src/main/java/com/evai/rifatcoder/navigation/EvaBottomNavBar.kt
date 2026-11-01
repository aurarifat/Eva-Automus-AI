package com.evai.rifatcoder.navigation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evai.rifatcoder.ui.theme.EvaTheme

@Composable
fun EvaBottomNavBar(
    currentRoute: String?,
    onNavigateToDestination: (EvaDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = EvaTheme.extendedColors.borderSubtle

    NavigationBar(
        modifier = modifier
            .drawBehind {
                // Subtle top border divider
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("eva_bottom_navigation_bar"),
        containerColor = EvaTheme.extendedColors.backgroundSecondary,
        contentColor = EvaTheme.extendedColors.textSecondary,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        EvaDestination.entries.forEach { destination ->
            val isSelected = currentRoute == destination.route
            val label = stringResource(destination.labelRes)
            val contentDescription = stringResource(destination.contentDescriptionRes)

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EvaTheme.extendedColors.primaryPurple,
                    selectedTextColor = EvaTheme.extendedColors.primaryPurple,
                    unselectedIconColor = EvaTheme.extendedColors.textSecondary,
                    unselectedTextColor = EvaTheme.extendedColors.textSecondary,
                    indicatorColor = EvaTheme.extendedColors.primaryPurple.copy(alpha = 0.15f),
                ),
                modifier = Modifier.testTag(destination.testTag),
            )
        }
    }
}
