package com.evai.rifatcoder.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.evai.rifatcoder.ui.chat.ChatViewModel
import com.evai.rifatcoder.ui.screens.AiConfigScreen
import com.evai.rifatcoder.ui.screens.AutomationScreen
import com.evai.rifatcoder.ui.screens.ChatScreen
import com.evai.rifatcoder.ui.screens.ConversationHistoryScreen
import com.evai.rifatcoder.ui.screens.HomeScreen
import com.evai.rifatcoder.ui.screens.MemoryScreen
import com.evai.rifatcoder.ui.screens.SettingsScreen
import com.evai.rifatcoder.ui.screens.VoiceScreen
import com.evai.rifatcoder.ui.screens.EdgeGlowSettingsScreen
import com.evai.rifatcoder.ui.screens.VoiceSettingsScreen

@Composable
fun EvaNavHost(
    navController: NavHostController,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = EvaDestination.HOME.route,
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues),
        enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) },
        exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) },
        popEnterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) },
        popExitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) },
    ) {
        composable(EvaDestination.HOME.route) {
            HomeScreen(
                onNavigateToChat = { navController.navigateToEvaDestination(EvaDestination.CHAT) },
                onNavigateToChatWithId = { convId -> navController.navigate("chat?convId=$convId") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToVoice = { navController.navigateToEvaDestination(EvaDestination.VOICE) },
                onNavigateToMemory = { navController.navigateToEvaDestination(EvaDestination.MEMORY) },
                onNavigateToSettings = { navController.navigateToEvaDestination(EvaDestination.SETTINGS) },
                onNavigateToHome = { navController.navigateToEvaDestination(EvaDestination.HOME) },
                onNavigateToAiConfig = { navController.navigate("ai_config") },
                onNavigateToAutomation = { navController.navigate("automation") },
            )
        }

        composable(
            route = "chat?convId={convId}",
            arguments = listOf(
                navArgument("convId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val convId = backStackEntry.arguments?.getString("convId")
            val chatViewModel: ChatViewModel = viewModel()

            LaunchedEffect(convId) {
                if (!convId.isNullOrBlank()) {
                    chatViewModel.loadConversation(convId)
                }
            }

            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToConfig = { navController.navigate("ai_config") },
                onNavigateToHistory = { navController.navigate("history") },
            )
        }

        composable("history") {
            ConversationHistoryScreen(
                onSelectConversation = { convId ->
                    navController.navigate("chat?convId=$convId") {
                        popUpTo(EvaDestination.HOME.route)
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(EvaDestination.VOICE.route) {
            VoiceScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { navController.navigateToEvaDestination(EvaDestination.CHAT) },
                onNavigateToSettings = { navController.navigate("voice_settings") },
            )
        }

        composable(EvaDestination.MEMORY.route) {
            MemoryScreen()
        }

        composable(EvaDestination.SETTINGS.route) {
            SettingsScreen(
                onNavigateToAiConfig = { navController.navigate("ai_config") },
                onNavigateToVoiceSettings = { navController.navigate("voice_settings") },
                onNavigateToAutomation = { navController.navigate("automation") },
                onNavigateToEdgeGlow = { navController.navigate("edge_glow_settings") },
                onNavigateToMemory = { navController.navigateToEvaDestination(EvaDestination.MEMORY) },
            )
        }

        composable("voice_settings") {
            VoiceSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable("ai_config") {
            AiConfigScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable("automation") {
            AutomationScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable("edge_glow_settings") {
            EdgeGlowSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

fun NavHostController.navigateToEvaDestination(destination: EvaDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
