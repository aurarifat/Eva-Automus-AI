package com.evai.rifatcoder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val EvaDarkColorScheme = darkColorScheme(
    primary = EvaPrimaryPurple,
    onPrimary = EvaTextPrimary,
    primaryContainer = EvaSurfaceElevated,
    onPrimaryContainer = EvaPrimaryPurple,
    secondary = EvaSecondaryCyan,
    onSecondary = EvaBackgroundMain,
    secondaryContainer = EvaSurfaceElevated,
    onSecondaryContainer = EvaSecondaryCyan,
    background = EvaBackgroundMain,
    onBackground = EvaTextPrimary,
    surface = EvaBackgroundSecondary,
    onSurface = EvaTextPrimary,
    surfaceVariant = EvaSurfaceCard,
    onSurfaceVariant = EvaTextSecondary,
    outline = EvaBorderSubtle,
    outlineVariant = EvaBorderActive,
    error = EvaError,
    onError = EvaTextPrimary,
)

object EvaTheme {
    val extendedColors: EvaExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalEvaExtendedColors.current

    val spacing: EvaSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalEvaSpacing.current

    val shapes: EvaExtendedShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalEvaShapes.current
}

@Composable
fun EvaAiTheme(
    content: @Composable () -> Unit,
) {
    val extendedColors = EvaExtendedColors()
    val spacing = EvaSpacing()
    val extendedShapes = EvaExtendedShapes()

    CompositionLocalProvider(
        LocalEvaExtendedColors provides extendedColors,
        LocalEvaSpacing provides spacing,
        LocalEvaShapes provides extendedShapes,
    ) {
        MaterialTheme(
            colorScheme = EvaDarkColorScheme,
            typography = EvaTypography,
            shapes = EvaShapes,
            content = content,
        )
    }
}
