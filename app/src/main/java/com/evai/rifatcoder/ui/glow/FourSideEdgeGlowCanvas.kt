package com.evai.rifatcoder.ui.glow

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.evai.rifatcoder.domain.glow.EdgeGlowColorOption
import com.evai.rifatcoder.domain.glow.EdgeGlowIntensity
import com.evai.rifatcoder.domain.glow.EdgeGlowPreferences
import com.evai.rifatcoder.domain.glow.EdgeGlowState

/**
 * Premium four-sided animated edge glow overlay.
 *
 * Renders a thin, elegant luminous boundary along all four screen edges (Top, Bottom, Left, Right)
 * with rounded display corners, soft outer glow, and audio-amplitude modulation.
 * The center remains 100% transparent and touch-pass-through.
 */
@Composable
fun FourSideEdgeGlowCanvas(
    glowState: EdgeGlowState,
    soundLevel: Float,
    preferences: EdgeGlowPreferences,
    modifier: Modifier = Modifier,
) {
    val shouldRender = preferences.shouldGlowForState(glowState)
    if (!shouldRender) {
        // Zero CPU/GPU rendering when glow is not active
        return
    }

    // Animation transition duration tailored to active state
    val cycleDurationMs = when (glowState) {
        EdgeGlowState.THINKING -> 1800
        EdgeGlowState.WAITING_FOR_CONFIRMATION -> 1600
        EdgeGlowState.EXECUTING -> 2000
        EdgeGlowState.SUCCESS -> 1400
        EdgeGlowState.FAILED, EdgeGlowState.ERROR -> 1000
        EdgeGlowState.CANCELLED -> 2200
        EdgeGlowState.LISTENING -> 3500
        EdgeGlowState.SPEAKING -> 2800
        EdgeGlowState.IDLE -> 5000
    }

    val infiniteTransition = rememberInfiniteTransition(label = "edge_glow_transition")

    // Continuous sweep angle around the screen perimeter (0f to 360f)
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "edge_glow_sweep",
    )

    // Breathing pulse animation for speaking, listening, confirmation, and success
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = when (glowState) {
            EdgeGlowState.SUCCESS -> 0.85f
            EdgeGlowState.WAITING_FOR_CONFIRMATION -> 0.65f
            EdgeGlowState.FAILED, EdgeGlowState.ERROR -> 0.60f
            else -> 0.75f
        },
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (glowState) {
                    EdgeGlowState.THINKING -> 600
                    EdgeGlowState.WAITING_FOR_CONFIRMATION -> 700
                    EdgeGlowState.SUCCESS -> 500
                    EdgeGlowState.FAILED, EdgeGlowState.ERROR -> 400
                    else -> 1100
                },
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "edge_glow_pulse",
    )

    val intensity = preferences.intensity
    val colorOption = preferences.colorOption
    val colors = colorOption.getColors(glowState)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("four_side_edge_glow_canvas")
    ) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // Audio amplitude modulation: scale pulse cleanly with true soundLevel (0.0 to 1.0)
        val audioScale = if (glowState == EdgeGlowState.SPEAKING || glowState == EdgeGlowState.LISTENING) {
            0.80f + (soundLevel.coerceIn(0f, 1f) * 0.40f) * pulseFactor
        } else if (glowState == EdgeGlowState.THINKING) {
            pulseFactor
        } else {
            1.0f
        }

        val baseOpacity = (intensity.maxOpacity * audioScale).coerceIn(0.1f, 1.0f)
        val outerGlowPx = intensity.outerGlowWidth.toPx()
        val coreStrokePx = intensity.coreStrokeWidth.toPx()

        // 28dp display corner rounding
        val cornerRadiusPx = 28.dp.toPx()

        // Construct rounded screen perimeter path
        val inset = outerGlowPx / 2f
        val perimeterPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = inset,
                    top = inset,
                    right = width - inset,
                    bottom = height - inset,
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                )
            )
        }

        // Center point for sweep gradient
        val center = Offset(width / 2f, height / 2f)

        // Rotate gradient brush according to animated sweepAngle
        val gradientBrush = Brush.sweepGradient(
            colors = colors.map { it.copy(alpha = it.alpha * baseOpacity) },
            center = center,
        )

        // 1. Soft outer diffuse glow layer
        drawPath(
            path = perimeterPath,
            brush = gradientBrush,
            alpha = 0.35f * baseOpacity,
            style = Stroke(width = outerGlowPx),
            blendMode = BlendMode.Screen,
        )

        // 2. Secondary smooth halo layer
        drawPath(
            path = perimeterPath,
            brush = gradientBrush,
            alpha = 0.65f * baseOpacity,
            style = Stroke(width = outerGlowPx * 0.55f),
        )

        // 3. Sharp luminous neon core line
        drawPath(
            path = perimeterPath,
            brush = gradientBrush,
            alpha = baseOpacity,
            style = Stroke(width = coreStrokePx),
        )
    }
}
