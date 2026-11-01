package com.evai.rifatcoder.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.evai.rifatcoder.navigation.EvaBottomNavBar
import com.evai.rifatcoder.navigation.EvaNavHost
import com.evai.rifatcoder.navigation.navigateToEvaDestination
import com.evai.rifatcoder.ui.components.EvaScreenBackground

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.evai.rifatcoder.data.glow.EdgeGlowPreferencesRepository
import com.evai.rifatcoder.data.glow.EdgeGlowStateHolder
import com.evai.rifatcoder.ui.glow.FourSideEdgeGlowCanvas

@Composable
fun EvaApp(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val glowPrefsRepo = remember { EdgeGlowPreferencesRepository(context) }
    val glowPrefs by glowPrefsRepo.preferences.collectAsState()
    val glowState by EdgeGlowStateHolder.glowState.collectAsState()
    val soundLevel by EdgeGlowStateHolder.soundLevel.collectAsState()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    EvaScreenBackground(
        modifier = modifier.testTag("eva_app_root"),
        applyWindowInsets = false,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentColor = Color.Unspecified,
                bottomBar = {
                    EvaBottomNavBar(
                        currentRoute = currentRoute,
                        onNavigateToDestination = { destination ->
                            navController.navigateToEvaDestination(destination)
                        },
                    )
                },
            ) { innerPadding ->
                EvaNavHost(
                    navController = navController,
                    paddingValues = innerPadding,
                )
            }

            // Four-Side Edge Glow Overlay (renders around all 4 display edges)
            FourSideEdgeGlowCanvas(
                glowState = glowState,
                soundLevel = soundLevel,
                preferences = glowPrefs,
            )
        }
    }
}
