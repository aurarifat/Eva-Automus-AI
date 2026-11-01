package com.evai.rifatcoder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.evai.rifatcoder.ui.theme.EvaTheme

@Composable
fun EvaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    testTag: String = "eva_primary_button",
) {
    Button(
        onClick = {
            if (!isLoading) onClick()
        },
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag(testTag),
        enabled = enabled && !isLoading,
        shape = EvaTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = EvaTheme.extendedColors.primaryPurple,
            contentColor = Color.White,
            disabledContainerColor = EvaTheme.extendedColors.surfaceElevated,
            disabledContentColor = EvaTheme.extendedColors.textDisabled,
        ),
        contentPadding = PaddingValues(
            horizontal = EvaTheme.spacing.standard,
            vertical = EvaTheme.spacing.medium,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
            Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
fun EvaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    containerColor: Color? = null,
    contentColor: Color? = null,
    testTag: String = "eva_secondary_button",
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag(testTag),
        enabled = enabled,
        shape = EvaTheme.shapes.medium,
        border = BorderStroke(
            1.dp,
            if (enabled) EvaTheme.extendedColors.borderActive else EvaTheme.extendedColors.borderSubtle,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor ?: EvaTheme.extendedColors.surfaceCard,
            contentColor = contentColor ?: EvaTheme.extendedColors.textPrimary,
            disabledContainerColor = EvaTheme.extendedColors.surfaceCard,
            disabledContentColor = EvaTheme.extendedColors.textDisabled,
        ),
        contentPadding = PaddingValues(
            horizontal = EvaTheme.spacing.standard,
            vertical = EvaTheme.spacing.medium,
        ),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (enabled) EvaTheme.extendedColors.secondaryCyan else EvaTheme.extendedColors.textDisabled,
            )
            Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
fun EvaIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = EvaTheme.extendedColors.textPrimary,
    containerColor: Color = Color.Transparent,
    testTag: String = "eva_icon_button",
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = EvaTheme.extendedColors.primaryPurple),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) tint else EvaTheme.extendedColors.textDisabled,
        )
    }
}
