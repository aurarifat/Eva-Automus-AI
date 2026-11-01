package com.evai.rifatcoder.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.evai.rifatcoder.R
import com.evai.rifatcoder.ui.components.EvaCard
import com.evai.rifatcoder.ui.components.EvaSectionHeader
import com.evai.rifatcoder.ui.components.EvaStatusIndicator
import com.evai.rifatcoder.ui.components.EvaStatusLevel
import androidx.compose.ui.platform.LocalContext
import com.evai.rifatcoder.data.security.ProviderConnectionStatus
import com.evai.rifatcoder.data.security.SecureConfigStorage
import com.evai.rifatcoder.ui.components.EvaTopBar
import com.evai.rifatcoder.ui.theme.EvaTheme

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToAiConfig: () -> Unit = {},
    onNavigateToVoiceSettings: () -> Unit = {},
    onNavigateToAutomation: () -> Unit = {},
    onNavigateToEdgeGlow: () -> Unit = {},
) {
    val context = LocalContext.current
    val storage = remember { SecureConfigStorage(context) }
    val activeProvider = storage.getActiveProvider()
    val hasKey = storage.hasApiKey(activeProvider)
    val status = storage.getProviderStatus(activeProvider)

    val (aiStatusLevel, aiStatusLabel) = when {
        status == ProviderConnectionStatus.CONNECTED -> 
            Pair(EvaStatusLevel.Available, "Connected (${activeProvider.displayName})")
        hasKey -> 
            Pair(EvaStatusLevel.InProgress, "Configured (${activeProvider.displayName})")
        else -> 
            Pair(EvaStatusLevel.NotConfigured, "Not Configured")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_settings"),
    ) {
        EvaTopBar(
            title = stringResource(R.string.title_settings),
            subtitle = stringResource(R.string.subtitle_settings),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EvaTheme.spacing.standard),
        ) {
            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            EvaSectionHeader(title = "Core Configuration")

            SettingsCategoryCard(
                icon = Icons.Default.SmartToy,
                title = "AI Provider",
                description = "Google Gemini & OmniRoute API keys, model presets, and live connection test.",
                status = aiStatusLevel,
                statusLabel = aiStatusLabel,
                onClick = onNavigateToAiConfig,
                testTag = "settings_ai_provider_card",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            SettingsCategoryCard(
                icon = Icons.Default.Mic,
                title = "Voice & Speech",
                description = "Speech recognition language, TTS pitch & rate, and 'Hey Eva' feasibility.",
                status = EvaStatusLevel.Available,
                statusLabel = "Configured",
                onClick = onNavigateToVoiceSettings,
                testTag = "settings_voice_speech_card",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            val isAccessibilityActive = remember {
                com.evai.rifatcoder.data.accessibility.EvaAccessibilityService.isServiceEnabled(context)
            }
            SettingsCategoryCard(
                icon = Icons.Default.TouchApp,
                title = "App Control & Automation",
                description = "Android Accessibility Service for hands-free control, screen reading, and app interaction.",
                status = if (isAccessibilityActive) EvaStatusLevel.Available else EvaStatusLevel.Disabled,
                statusLabel = if (isAccessibilityActive) "Active" else "Disabled",
                onClick = onNavigateToAutomation,
                testTag = "settings_automation_card",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            SettingsCategoryCard(
                icon = Icons.Default.Palette,
                title = "Appearance",
                description = "Luxury dark theme, accent colors (#8A7FFF & #00E5FF), animated orb styling.",
                status = EvaStatusLevel.Available,
                statusLabel = "Dark Active",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            val glowPrefsRepo = remember { com.evai.rifatcoder.data.glow.EdgeGlowPreferencesRepository(context) }
            val glowPrefs = glowPrefsRepo.preferences.value
            val hasOverlayPerm = remember { com.evai.rifatcoder.data.glow.EdgeGlowOverlayService.canDrawOverlays(context) }

            SettingsCategoryCard(
                icon = Icons.Default.DisplaySettings,
                title = "Edge Glow & Voice Status",
                description = "Four-sided animated screen border, visual voice & automation states, and overlay permission.",
                status = if (glowPrefs.isEnabled) EvaStatusLevel.Available else EvaStatusLevel.Disabled,
                statusLabel = if (glowPrefs.isEnabled) {
                    if (hasOverlayPerm && glowPrefs.isSystemOverlayEnabled) "Overlay Active" else "In-App Active"
                } else "Disabled",
                onClick = onNavigateToEdgeGlow,
                testTag = "settings_edge_glow_card",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            EvaSectionHeader(title = "Security & Privacy")

            SettingsCategoryCard(
                icon = Icons.Default.Lock,
                title = "Memory & Privacy",
                description = "Retention windows, local database encryption, memory inspection, and wipe.",
                status = EvaStatusLevel.NotConfigured,
                statusLabel = "Not Configured",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            SettingsCategoryCard(
                icon = Icons.Default.VpnKey,
                title = "Permissions",
                description = "Microphone access, post notifications, foreground service, accessibility overlay.",
                status = EvaStatusLevel.NotConfigured,
                statusLabel = "Not Requested",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            SettingsCategoryCard(
                icon = Icons.Default.Extension,
                title = "Integrations",
                description = "External connectors: Telegram bot, Gmail automated actions, custom webhooks.",
                status = EvaStatusLevel.Disabled,
                statusLabel = "Inactive",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            EvaSectionHeader(title = "System Information")

            EvaCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = EvaTheme.extendedColors.surfaceElevated,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SystemInfoRow(label = "Application", value = "EVA AI")
                    SystemInfoRow(label = "Developer", value = "Aura RIFAT")
                    SystemInfoRow(label = "Application ID", value = "com.evai.rifatcoder")
                    SystemInfoRow(label = "Version", value = "1.0.0-alpha (Phase 2)")
                    SystemInfoRow(label = "UI Architecture", value = "Jetpack Compose M3 + MVVM")
                    SystemInfoRow(label = "Target SDK", value = "Android 36 (Release 36.1)")
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.large))
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    icon: ImageVector,
    title: String,
    description: String,
    status: EvaStatusLevel,
    statusLabel: String,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
) {
    EvaCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        backgroundColor = EvaTheme.extendedColors.surfaceCard,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EvaTheme.extendedColors.primaryPurple,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(EvaTheme.spacing.standard))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EvaTheme.extendedColors.textPrimary,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    EvaStatusIndicator(
                        status = status,
                        customLabel = statusLabel,
                        showBorder = false,
                    )
                }
                Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EvaTheme.extendedColors.textSecondary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SystemInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = EvaTheme.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = EvaTheme.extendedColors.textSecondary,
            ),
            modifier = Modifier.weight(0.45f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = EvaTheme.extendedColors.textPrimary,
            ),
            modifier = Modifier.weight(0.55f),
        )
    }
}
