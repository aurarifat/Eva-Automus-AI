package com.evai.rifatcoder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.evai.rifatcoder.ui.theme.EvaTheme

@Composable
fun EvaScreenBackground(
    modifier: Modifier = Modifier,
    applyWindowInsets: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val bgColor = EvaTheme.extendedColors.backgroundMain
    val purpleAccent = EvaTheme.extendedColors.primaryPurple.copy(alpha = 0.08f)
    val cyanAccent = EvaTheme.extendedColors.secondaryCyan.copy(alpha = 0.05f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .drawBehind {
                // Subtle atmospheric lighting in the top-right and center-left
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(purpleAccent, Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.12f),
                        radius = size.width * 0.75f,
                    ),
                    radius = size.width * 0.75f,
                    center = Offset(size.width * 0.85f, size.height * 0.12f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(cyanAccent, Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.65f),
                        radius = size.width * 0.6f,
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * 0.15f, size.height * 0.65f),
                )
            }
            .then(
                if (applyWindowInsets) {
                    Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                } else {
                    Modifier
                }
            ),
        content = content,
    )
}
