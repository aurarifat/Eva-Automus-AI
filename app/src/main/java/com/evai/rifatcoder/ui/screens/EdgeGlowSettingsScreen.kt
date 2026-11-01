package com.evai.rifatcoder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.evai.rifatcoder.R
import com.evai.rifatcoder.data.glow.EdgeGlowOverlayService
import com.evai.rifatcoder.data.glow.EdgeGlowPreferencesRepository
import com.evai.rifatcoder.data.glow.EdgeGlowStateHolder
import com.evai.rifatcoder.domain.glow.EdgeGlowColorOption
import com.evai.rifatcoder.domain.glow.EdgeGlowIntensity
import com.evai.rifatcoder.domain.glow.EdgeGlowState
import com.evai.rifatcoder.ui.components.EvaCard
import com.evai.rifatcoder.ui.components.EvaPrimaryButton
import com.evai.rifatcoder.ui.components.EvaSectionHeader
import com.evai.rifatcoder.ui.components.EvaStatusIndicator
import com.evai.rifatcoder.ui.components.EvaStatusLevel
import com.evai.rifatcoder.ui.components.EvaTopBar
import com.evai.rifatcoder.ui.theme.EvaTheme

@Composable
fun EdgeGlowSettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember { EdgeGlowPreferencesRepository(context) }
    val preferences by repository.preferences.collectAsState()
    val activeGlowState by EdgeGlowStateHolder.glowState.collectAsState()

    var hasOverlayPermission by remember {
        mutableStateOf(EdgeGlowOverlayService.canDrawOverlays(context))
    }

    // Refresh permission when screen resumes
    LaunchedEffect(Unit) {
        hasOverlayPermission = EdgeGlowOverlayService.canDrawOverlays(context)
    }

    // Clean up simulation when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            EdgeGlowStateHolder.stopSimulation()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_edge_glow_settings"),
    ) {
        EvaTopBar(
            title = stringResource(R.string.title_edge_glow),
            subtitle = stringResource(R.string.subtitle_edge_glow),
            onBackClick = onNavigateBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EvaTheme.spacing.standard),
        ) {
            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // Master Enable Toggle Card
            EvaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_edge_glow_master"),
                backgroundColor = EvaTheme.extendedColors.surfaceCard,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.edge_glow_enable_label),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EvaTheme.extendedColors.textPrimary,
                            ),
                        )
                        Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))
                        Text(
                            text = stringResource(R.string.edge_glow_enable_desc),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = EvaTheme.extendedColors.textSecondary,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.width(EvaTheme.spacing.standard))

                    Switch(
                        checked = preferences.isEnabled,
                        onCheckedChange = { isChecked ->
                            repository.setEnabled(isChecked)
                            if (isChecked && preferences.isSystemOverlayEnabled && hasOverlayPermission) {
                                EdgeGlowOverlayService.startService(context)
                            } else {
                                EdgeGlowOverlayService.stopService(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EvaTheme.extendedColors.textPrimary,
                            checkedTrackColor = EvaTheme.extendedColors.secondaryCyan,
                            uncheckedThumbColor = EvaTheme.extendedColors.textDisabled,
                            uncheckedTrackColor = EvaTheme.extendedColors.surfaceElevated,
                        ),
                        modifier = Modifier.testTag("switch_edge_glow_master"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // Live Simulator Card
            EvaSectionHeader(title = stringResource(R.string.edge_glow_preview_title))

            EvaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_edge_glow_simulator"),
                backgroundColor = EvaTheme.extendedColors.surfaceElevated,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.edge_glow_preview_desc),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EvaTheme.extendedColors.textSecondary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StateSimulateChip(
                            label = "Idle",
                            isSelected = activeGlowState == EdgeGlowState.IDLE,
                            onClick = { EdgeGlowStateHolder.simulateState(EdgeGlowState.IDLE, 0f) },
                            modifier = Modifier.weight(1f),
                            testTag = "chip_sim_idle",
                        )
                        StateSimulateChip(
                            label = "Listening",
                            isSelected = activeGlowState == EdgeGlowState.LISTENING,
                            onClick = { EdgeGlowStateHolder.simulateState(EdgeGlowState.LISTENING, 0.7f) },
                            modifier = Modifier.weight(1f),
                            testTag = "chip_sim_listening",
                        )
                        StateSimulateChip(
                            label = "Thinking",
                            isSelected = activeGlowState == EdgeGlowState.THINKING,
                            onClick = { EdgeGlowStateHolder.simulateState(EdgeGlowState.THINKING, 0.4f) },
                            modifier = Modifier.weight(1f),
                            testTag = "chip_sim_thinking",
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StateSimulateChip(
                            label = "Speaking",
                            isSelected = activeGlowState == EdgeGlowState.SPEAKING,
                            onClick = { EdgeGlowStateHolder.simulateState(EdgeGlowState.SPEAKING, 0.85f) },
                            modifier = Modifier.weight(1f),
                            testTag = "chip_sim_speaking",
                        )
                        StateSimulateChip(
                            label = "Executing",
                            isSelected = activeGlowState == EdgeGlowState.EXECUTING,
                            onClick = { EdgeGlowStateHolder.simulateState(EdgeGlowState.EXECUTING, 0.5f) },
                            modifier = Modifier.weight(1f),
                            testTag = "chip_sim_executing",
                        )
                        StateSimulateChip(
                            label = "Error",
                            isSelected = activeGlowState == EdgeGlowState.ERROR,
                            onClick = { EdgeGlowStateHolder.simulateState(EdgeGlowState.ERROR, 0.3f) },
                            modifier = Modifier.weight(1f),
                            testTag = "chip_sim_error",
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // Intensity Setting
            EvaSectionHeader(title = stringResource(R.string.edge_glow_intensity_label))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EdgeGlowIntensity.entries.forEach { level ->
                    val isSelected = preferences.intensity == level
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) EvaTheme.extendedColors.primaryPurple.copy(alpha = 0.25f)
                                else EvaTheme.extendedColors.surfaceElevated
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) EvaTheme.extendedColors.primaryPurple
                                else EvaTheme.extendedColors.borderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { repository.setIntensity(level) }
                            .padding(vertical = 12.dp)
                            .testTag("chip_intensity_${level.id}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = level.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) EvaTheme.extendedColors.primaryPurple
                                    else EvaTheme.extendedColors.textSecondary,
                                ),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${level.coreStrokeWidth.value}dp",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = EvaTheme.extendedColors.textDisabled,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // Color Option Setting
            EvaSectionHeader(title = stringResource(R.string.edge_glow_color_label))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EdgeGlowColorOption.entries.forEach { option ->
                    val isSelected = preferences.colorOption == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) EvaTheme.extendedColors.secondaryCyan.copy(alpha = 0.20f)
                                else EvaTheme.extendedColors.surfaceElevated
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) EvaTheme.extendedColors.secondaryCyan
                                else EvaTheme.extendedColors.borderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { repository.setColorOption(option) }
                            .padding(vertical = 12.dp)
                            .testTag("chip_color_${option.id}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = option.displayName.substringBefore(" ("),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) EvaTheme.extendedColors.secondaryCyan
                                else EvaTheme.extendedColors.textSecondary,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // State Triggers (Checkboxes / Switches)
            EvaSectionHeader(title = stringResource(R.string.edge_glow_triggers_header))

            TriggerToggleRow(
                title = stringResource(R.string.edge_glow_trigger_listening),
                checked = preferences.showDuringListening,
                onCheckedChange = { repository.setShowDuringListening(it) },
                testTag = "switch_trigger_listening",
            )
            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            TriggerToggleRow(
                title = stringResource(R.string.edge_glow_trigger_speaking),
                checked = preferences.showDuringSpeaking,
                onCheckedChange = { repository.setShowDuringSpeaking(it) },
                testTag = "switch_trigger_speaking",
            )
            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            TriggerToggleRow(
                title = stringResource(R.string.edge_glow_trigger_thinking),
                checked = preferences.showDuringThinking,
                onCheckedChange = { repository.setShowDuringThinking(it) },
                testTag = "switch_trigger_thinking",
            )
            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            TriggerToggleRow(
                title = stringResource(R.string.edge_glow_trigger_executing),
                checked = preferences.showDuringExecuting,
                onCheckedChange = { repository.setShowDuringExecuting(it) },
                testTag = "switch_trigger_executing",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // System Overlay Permission Card
            EvaSectionHeader(title = stringResource(R.string.edge_glow_overlay_perm_title))

            EvaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_system_overlay"),
                backgroundColor = EvaTheme.extendedColors.surfaceCard,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasOverlayPermission) stringResource(R.string.edge_glow_overlay_active)
                                else stringResource(R.string.edge_glow_overlay_inapp_only),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasOverlayPermission) EvaTheme.extendedColors.secondaryCyan
                                    else EvaTheme.extendedColors.textSecondary,
                                ),
                            )
                        }
                        EvaStatusIndicator(
                            status = if (hasOverlayPermission && preferences.isSystemOverlayEnabled)
                                EvaStatusLevel.Available
                            else EvaStatusLevel.Disabled,
                            customLabel = if (hasOverlayPermission && preferences.isSystemOverlayEnabled)
                                "Enabled"
                            else "Inactive",
                        )
                    }

                    Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

                    Text(
                        text = stringResource(R.string.edge_glow_overlay_perm_desc),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EvaTheme.extendedColors.textSecondary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

                    if (!hasOverlayPermission) {
                        EvaPrimaryButton(
                            text = stringResource(R.string.edge_glow_overlay_perm_btn),
                            onClick = {
                                EdgeGlowOverlayService.openOverlaySettings(context)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_configure_overlay_perm"),
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Enable System Overlay",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = EvaTheme.extendedColors.textPrimary,
                                ),
                            )
                            Switch(
                                checked = preferences.isSystemOverlayEnabled,
                                onCheckedChange = { isChecked ->
                                    repository.setSystemOverlayEnabled(isChecked)
                                    if (isChecked) {
                                        EdgeGlowOverlayService.startService(context)
                                    } else {
                                        EdgeGlowOverlayService.stopService(context)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EvaTheme.extendedColors.textPrimary,
                                    checkedTrackColor = EvaTheme.extendedColors.secondaryCyan,
                                    uncheckedThumbColor = EvaTheme.extendedColors.textDisabled,
                                    uncheckedTrackColor = EvaTheme.extendedColors.surfaceElevated,
                                ),
                                modifier = Modifier.testTag("switch_system_overlay"),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // Reset Button
            EvaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { repository.resetToDefaults() }
                    .testTag("btn_reset_edge_glow"),
                backgroundColor = EvaTheme.extendedColors.surfaceElevated,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = EvaTheme.extendedColors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
                    Text(
                        text = stringResource(R.string.edge_glow_reset_button),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = EvaTheme.extendedColors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.extraLarge))
        }
    }
}

@Composable
private fun StateSimulateChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) EvaTheme.extendedColors.primaryPurple.copy(alpha = 0.35f)
                else EvaTheme.extendedColors.surfaceCard
            )
            .border(
                1.dp,
                if (isSelected) EvaTheme.extendedColors.secondaryCyan else EvaTheme.extendedColors.borderSubtle,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) EvaTheme.extendedColors.secondaryCyan else EvaTheme.extendedColors.textPrimary,
            ),
        )
    }
}

@Composable
private fun TriggerToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    EvaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = EvaTheme.extendedColors.surfaceElevated,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = EvaTheme.extendedColors.textPrimary,
                ),
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = EvaTheme.extendedColors.textPrimary,
                    checkedTrackColor = EvaTheme.extendedColors.primaryPurple,
                    uncheckedThumbColor = EvaTheme.extendedColors.textDisabled,
                    uncheckedTrackColor = EvaTheme.extendedColors.surfaceCard,
                ),
                modifier = Modifier.testTag(testTag),
            )
        }
    }
}
