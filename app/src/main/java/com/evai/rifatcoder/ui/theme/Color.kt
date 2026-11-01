package com.evai.rifatcoder.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Background Tokens
val EvaBackgroundMain = Color(0xFF111019)
val EvaBackgroundSecondary = Color(0xFF171622)

// Surface Tokens
val EvaSurfaceCard = Color(0xFF1C1B27)
val EvaSurfaceElevated = Color(0xFF242231)
val EvaBorderSubtle = Color(0xFF2E2B3E)
val EvaBorderActive = Color(0xFF4A4466)

// Accent Tokens
val EvaPrimaryPurple = Color(0xFF8A7FFF)
val EvaSecondaryCyan = Color(0xFF00E5FF)
val EvaPrimaryPurpleVariant = Color(0xFF6C60F6)
val EvaSecondaryCyanVariant = Color(0xFF00B8D4)

// Text Tokens
val EvaTextPrimary = Color(0xFFFFFFFF)
val EvaTextSecondary = Color(0xFFB7B5C7)
val EvaTextDisabled = Color(0xFF5E5B73)

// Semantic Tokens
val EvaSuccess = Color(0xFF10B981)
val EvaWarning = Color(0xFFF59E0B)
val EvaError = Color(0xFFEF4444)
val EvaInfo = Color(0xFF00E5FF)

@Immutable
data class EvaExtendedColors(
    val backgroundMain: Color = EvaBackgroundMain,
    val backgroundSecondary: Color = EvaBackgroundSecondary,
    val surfaceCard: Color = EvaSurfaceCard,
    val surfaceElevated: Color = EvaSurfaceElevated,
    val borderSubtle: Color = EvaBorderSubtle,
    val borderActive: Color = EvaBorderActive,
    val primaryPurple: Color = EvaPrimaryPurple,
    val secondaryCyan: Color = EvaSecondaryCyan,
    val textPrimary: Color = EvaTextPrimary,
    val textSecondary: Color = EvaTextSecondary,
    val textDisabled: Color = EvaTextDisabled,
    val success: Color = EvaSuccess,
    val warning: Color = EvaWarning,
    val error: Color = EvaError,
    val info: Color = EvaInfo,
)

val LocalEvaExtendedColors = staticCompositionLocalOf { EvaExtendedColors() }
