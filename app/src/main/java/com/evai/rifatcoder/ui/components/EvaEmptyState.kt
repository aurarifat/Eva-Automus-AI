package com.evai.rifatcoder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.evai.rifatcoder.ui.theme.EvaTheme

@Composable
fun EvaEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    statusLevel: EvaStatusLevel? = null,
    statusLabel: String? = null,
    actionButton: (@Composable () -> Unit)? = null,
    testTag: String = "eva_empty_state",
) {
    EvaCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        contentPadding = EvaTheme.spacing.large,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = EvaTheme.extendedColors.surfaceElevated,
                        shape = CircleShape,
                    )
                    .border(
                        width = 1.dp,
                        color = EvaTheme.extendedColors.primaryPurple.copy(alpha = 0.3f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = EvaTheme.extendedColors.primaryPurple,
                )
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = EvaTheme.extendedColors.textPrimary,
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = EvaTheme.extendedColors.textSecondary,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.padding(horizontal = EvaTheme.spacing.small),
            )

            if (statusLevel != null) {
                Spacer(modifier = Modifier.height(EvaTheme.spacing.medium))
                EvaStatusIndicator(
                    status = statusLevel,
                    customLabel = statusLabel,
                )
            }

            if (actionButton != null) {
                Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))
                actionButton()
            }
        }
    }
}
