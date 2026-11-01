package com.evai.rifatcoder.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.evai.rifatcoder.R

enum class EvaDestination(
    val route: String,
    @get:StringRes val labelRes: Int,
    @get:StringRes val contentDescriptionRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
) {
    HOME(
        route = "home",
        labelRes = R.string.nav_home,
        contentDescriptionRes = R.string.cd_nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_item_home",
    ),
    CHAT(
        route = "chat",
        labelRes = R.string.nav_chat,
        contentDescriptionRes = R.string.cd_nav_chat,
        selectedIcon = Icons.Filled.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline,
        testTag = "nav_item_chat",
    ),
    VOICE(
        route = "voice",
        labelRes = R.string.nav_voice,
        contentDescriptionRes = R.string.cd_nav_voice,
        selectedIcon = Icons.Filled.Mic,
        unselectedIcon = Icons.Outlined.MicNone,
        testTag = "nav_item_voice",
    ),
    MEMORY(
        route = "memory",
        labelRes = R.string.nav_memory,
        contentDescriptionRes = R.string.cd_nav_memory,
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Psychology,
        testTag = "nav_item_memory",
    ),
    SETTINGS(
        route = "settings",
        labelRes = R.string.nav_settings,
        contentDescriptionRes = R.string.cd_nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_item_settings",
    ),
}
