package com.evai.rifatcoder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.evai.rifatcoder.ui.theme.EvaTheme

@Composable
fun EvaCard(
    modifier: Modifier = Modifier,
    shape: Shape = EvaTheme.shapes.large,
    backgroundColor: Color = EvaTheme.extendedColors.surfaceCard,
    borderColor: Color? = EvaTheme.extendedColors.borderSubtle,
    borderWidth: Dp = 1.dp,
    contentPadding: Dp = EvaTheme.spacing.standard,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val border = borderColor?.let { BorderStroke(borderWidth, it) }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor,
                contentColor = EvaTheme.extendedColors.textPrimary,
            ),
            border = border,
        ) {
            Box(
                modifier = Modifier.padding(contentPadding),
                content = content,
            )
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor,
                contentColor = EvaTheme.extendedColors.textPrimary,
            ),
            border = border,
        ) {
            Box(
                modifier = Modifier.padding(contentPadding),
                content = content,
            )
        }
    }
}
