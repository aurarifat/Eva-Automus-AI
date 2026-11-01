package com.evai.rifatcoder.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.evai.rifatcoder.data.voice.CentralVoiceState
import com.evai.rifatcoder.data.voice.CentralVoiceStateManager
import com.evai.rifatcoder.ui.theme.EvaTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium Holographic Particle Orb for EVA AI.
 * Visually reflects the single authoritative CentralVoiceState:
 * - IDLE: Calm breathing and ambient orbital ring rotation.
 * - STARTING: Soft anticipatory flare.
 * - LISTENING: Active pulsing, directly reactive to live microphone amplitude.
 * - PROCESSING: Swift swirling energy and intellectual shimmering.
 * - SPEAKING: Harmonic speech cadence pulses.
 * - EXECUTING_TASK: Energetic amber/cyan focused task animation.
 * - ERROR: Restrained crimson/amber alert indication.
 * - STOPPING: Smooth fade down to IDLE.
 */
@Composable
fun EvaAnimatedOrb(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    voiceState: CentralVoiceState = CentralVoiceStateManager.state.collectAsState().value,
    soundLevel: Float = CentralVoiceStateManager.soundLevel.collectAsState().value,
    testTag: String = "eva_animated_orb",
    onClick: (() -> Unit)? = null,
) {
    val transition = rememberInfiniteTransition(label = "eva_orb_transition")

    val pulseDuration = when (voiceState) {
        CentralVoiceState.IDLE -> 2800
        CentralVoiceState.STARTING -> 1400
        CentralVoiceState.LISTENING -> 1200
        CentralVoiceState.PROCESSING -> 900
        CentralVoiceState.SPEAKING -> 1100
        CentralVoiceState.EXECUTING_TASK -> 700
        CentralVoiceState.ERROR -> 1800
        CentralVoiceState.STOPPING -> 2200
    }

    val ring1Duration = when (voiceState) {
        CentralVoiceState.IDLE -> 10000
        CentralVoiceState.STARTING -> 6000
        CentralVoiceState.LISTENING -> 5000
        CentralVoiceState.PROCESSING -> 2400
        CentralVoiceState.SPEAKING -> 4500
        CentralVoiceState.EXECUTING_TASK -> 1800
        CentralVoiceState.ERROR -> 16000
        CentralVoiceState.STOPPING -> 12000
    }

    val ring2Duration = when (voiceState) {
        CentralVoiceState.IDLE -> 14000
        CentralVoiceState.STARTING -> 9000
        CentralVoiceState.LISTENING -> 7000
        CentralVoiceState.PROCESSING -> 3400
        CentralVoiceState.SPEAKING -> 6000
        CentralVoiceState.EXECUTING_TASK -> 2600
        CentralVoiceState.ERROR -> 22000
        CentralVoiceState.STOPPING -> 16000
    }

    // Breathing scale for inner orb
    val pulseScale by transition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    // Orbital ring rotations
    val ringRotation1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ring1Duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring1",
    )

    val ringRotation2 by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ring2Duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring2",
    )

    // Color definitions based on central state
    val (primaryColor, secondaryColor) = when (voiceState) {
        CentralVoiceState.IDLE -> Pair(
            EvaTheme.extendedColors.primaryPurple,
            EvaTheme.extendedColors.secondaryCyan
        )
        CentralVoiceState.STARTING -> Pair(
            EvaTheme.extendedColors.secondaryCyan,
            EvaTheme.extendedColors.primaryPurple
        )
        CentralVoiceState.LISTENING -> Pair(
            EvaTheme.extendedColors.secondaryCyan,
            Color(0xFF00B0FF)
        )
        CentralVoiceState.PROCESSING -> Pair(
            Color(0xFFBB86FC),
            EvaTheme.extendedColors.secondaryCyan
        )
        CentralVoiceState.SPEAKING -> Pair(
            EvaTheme.extendedColors.secondaryCyan,
            EvaTheme.extendedColors.primaryPurple
        )
        CentralVoiceState.EXECUTING_TASK -> Pair(
            Color(0xFFFFB300), // Amber gold
            EvaTheme.extendedColors.secondaryCyan
        )
        CentralVoiceState.ERROR -> Pair(
            Color(0xFFEF5350), // Crimson
            Color(0xFFFFA726)  // Warning amber
        )
        CentralVoiceState.STOPPING -> Pair(
            EvaTheme.extendedColors.primaryPurple.copy(alpha = 0.6f),
            EvaTheme.extendedColors.secondaryCyan.copy(alpha = 0.4f)
        )
    }

    // Amplitude modulation (reacts to real mic volume or TTS audio)
    val amplitudeBoost = when (voiceState) {
        CentralVoiceState.LISTENING -> soundLevel.coerceIn(0f, 1f) * 0.32f
        CentralVoiceState.SPEAKING -> 0.12f
        CentralVoiceState.EXECUTING_TASK -> 0.15f
        else -> 0f
    }
    val effectiveScale = pulseScale + amplitudeBoost

    val clickModifier = if (onClick != null) {
        Modifier
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                contentDescription = "EVA AI Holographic Orb - ${voiceState.displayName}"
            }
    } else {
        Modifier.semantics {
            contentDescription = "EVA AI Holographic Orb - ${voiceState.displayName}"
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .then(clickModifier)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = (this.size.minDimension / 2f) * 0.72f

            // Outer atmospheric soft aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.38f * effectiveScale),
                        secondaryColor.copy(alpha = 0.16f * effectiveScale),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius * 1.55f * effectiveScale,
                ),
                radius = radius * 1.55f * effectiveScale,
                center = center,
            )

            // Outer rotating ring with primary tint
            rotate(degrees = ringRotation1, pivot = center) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.50f),
                    radius = radius * 1.15f,
                    center = center,
                    style = Stroke(width = 1.8.dp.toPx()),
                )
                // Small satellite bead on ring 1
                val angleRad = Math.toRadians(45.0)
                val satellitePos = Offset(
                    x = center.x + (radius * 1.15f * cos(angleRad)).toFloat(),
                    y = center.y + (radius * 1.15f * sin(angleRad)).toFloat(),
                )
                drawCircle(
                    color = primaryColor,
                    radius = 3.5.dp.toPx() * (1f + amplitudeBoost),
                    center = satellitePos,
                )
            }

            // Middle counter-rotating ring with secondary tint
            rotate(degrees = ringRotation2, pivot = center) {
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.45f),
                    radius = radius * 0.95f,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx()),
                )
                val angleRad2 = Math.toRadians(210.0)
                val satellitePos2 = Offset(
                    x = center.x + (radius * 0.95f * cos(angleRad2)).toFloat(),
                    y = center.y + (radius * 0.95f * sin(angleRad2)).toFloat(),
                )
                drawCircle(
                    color = secondaryColor,
                    radius = 3.0.dp.toPx() * (1f + amplitudeBoost),
                    center = satellitePos2,
                )
            }

            // Inner glowing nucleus
            val coreRadius = radius * 0.65f * effectiveScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.96f),
                        primaryColor.copy(alpha = 0.88f),
                        secondaryColor.copy(alpha = 0.78f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = coreRadius,
                ),
                radius = coreRadius,
                center = center,
            )

            // Crisp center spark / specular flare
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = 4.2.dp.toPx() * effectiveScale,
                center = center,
            )
        }
    }
}
