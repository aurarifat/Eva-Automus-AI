package com.evai.rifatcoder.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.evai.rifatcoder.ui.theme.EvaTheme

@Composable
fun EvaLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    primaryColor: Color = EvaTheme.extendedColors.primaryPurple,
    secondaryColor: Color = EvaTheme.extendedColors.secondaryCyan,
    strokeWidth: Dp = 3.dp,
    testTag: String = "eva_loading_indicator",
) {
    val transition = rememberInfiniteTransition(label = "eva_loading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )
    val sweep by transition.animateFloat(
        initialValue = 45f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sweep",
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            // Background track
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f),
                style = Stroke(width = strokePx),
            )
            // Animated primary arc
            drawArc(
                color = primaryColor,
                startAngle = rotation,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            // Secondary counter arc
            drawArc(
                color = secondaryColor,
                startAngle = (rotation + 180f) % 360f,
                sweepAngle = 60f,
                useCenter = false,
                style = Stroke(width = strokePx * 0.8f, cap = StrokeCap.Round),
            )
        }
    }
}
