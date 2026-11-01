package com.evai.rifatcoder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.evai.rifatcoder.R
import com.evai.rifatcoder.ui.theme.EvaTheme

enum class EvaStatusLevel {
    Available,
    NotConfigured,
    Disabled,
    Error,
    InProgress,
}

@Composable
fun EvaStatusIndicator(
    status: EvaStatusLevel,
    modifier: Modifier = Modifier,
    customLabel: String? = null,
    showBorder: Boolean = true,
) {
    val (color, icon, defaultStringRes) = when (status) {
        EvaStatusLevel.Available -> Triple(
            EvaTheme.extendedColors.success,
            Icons.Default.CheckCircle,
            R.string.status_available,
        )
        EvaStatusLevel.NotConfigured -> Triple(
            EvaTheme.extendedColors.warning,
            Icons.Default.Info,
            R.string.status_not_configured,
        )
        EvaStatusLevel.Disabled -> Triple(
            EvaTheme.extendedColors.textDisabled,
            Icons.Default.Close,
            R.string.status_disabled,
        )
        EvaStatusLevel.Error -> Triple(
            EvaTheme.extendedColors.error,
            Icons.Default.Warning,
            R.string.status_error,
        )
        EvaStatusLevel.InProgress -> Triple(
            EvaTheme.extendedColors.secondaryCyan,
            Icons.Default.HourglassEmpty,
            R.string.status_in_progress,
        )
    }

    val labelText = customLabel ?: stringResource(defaultStringRes)

    Row(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(100.dp),
            )
            .then(
                if (showBorder) {
                    Modifier.border(
                        width = 1.dp,
                        color = color.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(100.dp),
                    )
                } else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("eva_status_indicator"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = labelText,
            modifier = Modifier.size(14.dp),
            tint = color,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = labelText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = color,
            ),
        )
    }
}
