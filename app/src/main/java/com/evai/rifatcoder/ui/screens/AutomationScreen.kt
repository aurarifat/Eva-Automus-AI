package com.evai.rifatcoder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evai.rifatcoder.R
import com.evai.rifatcoder.domain.automation.AutomationTaskStatus
import com.evai.rifatcoder.ui.automation.AutomationViewModel
import com.evai.rifatcoder.ui.components.EvaCard
import com.evai.rifatcoder.ui.components.EvaPrimaryButton
import com.evai.rifatcoder.ui.components.EvaSectionHeader
import com.evai.rifatcoder.ui.components.EvaStatusIndicator
import com.evai.rifatcoder.ui.components.EvaStatusLevel
import com.evai.rifatcoder.ui.components.EvaTopBar
import com.evai.rifatcoder.ui.theme.EvaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutomationScreen(
    modifier: Modifier = Modifier,
    viewModel: AutomationViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshServiceStatus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_automation"),
    ) {
        // Top App Bar
        EvaTopBar(
            title = stringResource(R.string.title_automation),
            subtitle = stringResource(R.string.subtitle_automation),
            onBackClick = onNavigateBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EvaTheme.spacing.standard),
        ) {
            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // 1. Accessibility Service Status Banner
            AccessibilityServiceBanner(
                isServiceEnabled = uiState.isServiceEnabled,
                onOpenSettings = { viewModel.openAccessibilitySettings() },
                onRefresh = { viewModel.refreshServiceStatus() },
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // 2. Active Task Execution Card
            AnimatedVisibility(visible = uiState.taskStatus != AutomationTaskStatus.Idle) {
                ActiveTaskCard(
                    status = uiState.taskStatus,
                    prompt = uiState.activePrompt ?: "",
                    currentDescription = uiState.currentActionDescription ?: "",
                    completedSteps = uiState.completedStepsCount,
                    totalSteps = uiState.totalStepsCount,
                    onStop = { viewModel.cancelActiveTask() },
                )
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // 3. Quick Automation Commands Section
            EvaSectionHeader(title = stringResource(R.string.automation_section_quick_actions))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EvaTheme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(EvaTheme.spacing.small),
            ) {
                QuickCommandChip(
                    icon = Icons.Default.Launch,
                    label = "Open YouTube",
                    onClick = { viewModel.submitCommand("Open YouTube") },
                    testTag = "quick_cmd_youtube",
                )
                QuickCommandChip(
                    icon = Icons.Default.Settings,
                    label = "Open Settings",
                    onClick = { viewModel.submitCommand("Open Settings") },
                    testTag = "quick_cmd_settings",
                )
                QuickCommandChip(
                    icon = Icons.Default.Visibility,
                    label = "Inspect Screen",
                    onClick = { viewModel.submitCommand("Inspect active screen") },
                    testTag = "quick_cmd_inspect",
                )
                QuickCommandChip(
                    icon = Icons.Default.TouchApp,
                    label = "Scroll Down",
                    onClick = { viewModel.submitCommand("Scroll down") },
                    testTag = "quick_cmd_scroll_down",
                )
                QuickCommandChip(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "Go Back",
                    onClick = { viewModel.submitCommand("Go back") },
                    testTag = "quick_cmd_back",
                )
                QuickCommandChip(
                    icon = Icons.Default.Home,
                    label = "Go Home",
                    onClick = { viewModel.submitCommand("Go home") },
                    testTag = "quick_cmd_home",
                )
                QuickCommandChip(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = "Volume Up",
                    onClick = { viewModel.submitCommand("Volume up") },
                    testTag = "quick_cmd_volume_up",
                )
                QuickCommandChip(
                    icon = Icons.Default.Alarm,
                    label = "Set Alarm (8:00 AM)",
                    onClick = { viewModel.submitCommand("Set alarm for 08:00 AM") },
                    testTag = "quick_cmd_alarm",
                )
                QuickCommandChip(
                    icon = Icons.Default.BatteryChargingFull,
                    label = "Check Battery",
                    onClick = { viewModel.submitCommand("Check battery status") },
                    testTag = "quick_cmd_battery",
                )
                QuickCommandChip(
                    icon = Icons.Default.FlashlightOn,
                    label = "Toggle Flashlight",
                    onClick = { viewModel.submitCommand("Toggle flashlight") },
                    testTag = "quick_cmd_flashlight",
                )
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // 4. System Access & Capability Matrix
            EvaSectionHeader(title = stringResource(R.string.automation_section_permissions))

            EvaCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = EvaTheme.extendedColors.surfaceCard,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CapabilityRow(
                        title = "Android Accessibility Service",
                        detail = if (uiState.isServiceEnabled) "Active & Bound" else "Authorization Required",
                        isGranted = uiState.isServiceEnabled,
                    )
                    CapabilityRow(
                        title = "App Launching Intent Dispatch",
                        detail = "Package visibility declared",
                        isGranted = true,
                    )
                    CapabilityRow(
                        title = "Audio Volume Adjustment",
                        detail = "AudioManager STREAM_MUSIC",
                        isGranted = true,
                    )
                    CapabilityRow(
                        title = "System Alarm Scheduling",
                        detail = "AlarmClock ACTION_SET_ALARM",
                        isGranted = true,
                    )
                    CapabilityRow(
                        title = "Camera Flashlight Control",
                        detail = "CameraManager setTorchMode",
                        isGranted = true,
                    )
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // 5. Task Audit & History
            EvaSectionHeader(title = stringResource(R.string.automation_section_history))

            if (uiState.taskHistory.isEmpty()) {
                EvaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = EvaTheme.extendedColors.surfaceElevated,
                ) {
                    Text(
                        text = stringResource(R.string.automation_empty_history),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EvaTheme.extendedColors.textSecondary,
                        ),
                    )
                }
            } else {
                uiState.taskHistory.forEach { report ->
                    TaskHistoryCard(report = report)
                    Spacer(modifier = Modifier.height(EvaTheme.spacing.small))
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.large))
        }
    }

    // Confirmation Dialog for Consequential Actions
    if (uiState.taskStatus == AutomationTaskStatus.WaitingForConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelActiveTask() },
            title = {
                Text(
                    text = stringResource(R.string.automation_confirm_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = EvaTheme.extendedColors.primaryPurple,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.automation_confirm_desc,
                        uiState.pendingConfirmationReason ?: "External operation"
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EvaTheme.extendedColors.textSecondary,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmPendingTask() },
                    modifier = Modifier.testTag("automation_btn_confirm_proceed"),
                ) {
                    Text(
                        text = stringResource(R.string.automation_confirm_proceed),
                        color = EvaTheme.extendedColors.secondaryCyan,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelActiveTask() },
                    modifier = Modifier.testTag("automation_btn_confirm_cancel"),
                ) {
                    Text(
                        text = stringResource(R.string.automation_confirm_cancel),
                        color = EvaTheme.extendedColors.textDisabled,
                    )
                }
            },
            containerColor = EvaTheme.extendedColors.surfaceElevated,
        )
    }
}

@Composable
private fun AccessibilityServiceBanner(
    isServiceEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    val borderColor = if (isServiceEnabled) {
        EvaTheme.extendedColors.secondaryCyan.copy(alpha = 0.5f)
    } else {
        EvaTheme.extendedColors.primaryPurple.copy(alpha = 0.5f)
    }

    EvaCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .testTag("banner_accessibility_service"),
        backgroundColor = EvaTheme.extendedColors.surfaceElevated,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isServiceEnabled) Icons.Default.CheckCircle else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isServiceEnabled) EvaTheme.extendedColors.secondaryCyan else EvaTheme.extendedColors.primaryPurple,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
                    Text(
                        text = if (isServiceEnabled) {
                            stringResource(R.string.automation_status_enabled)
                        } else {
                            stringResource(R.string.automation_status_disabled)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EvaTheme.extendedColors.textPrimary,
                        ),
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("automation_btn_recheck"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.automation_btn_recheck),
                        tint = EvaTheme.extendedColors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            Text(
                text = if (isServiceEnabled) {
                    stringResource(R.string.automation_status_desc_enabled)
                } else {
                    stringResource(R.string.automation_status_desc_disabled)
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EvaTheme.extendedColors.textSecondary,
                ),
            )

            if (!isServiceEnabled) {
                Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

                EvaPrimaryButton(
                    text = stringResource(R.string.automation_btn_open_settings),
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("automation_btn_open_settings"),
                )
            }
        }
    }
}

@Composable
private fun ActiveTaskCard(
    status: AutomationTaskStatus,
    prompt: String,
    currentDescription: String,
    completedSteps: Int,
    totalSteps: Int,
    onStop: () -> Unit,
) {
    EvaCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, EvaTheme.extendedColors.secondaryCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .testTag("card_active_task"),
        backgroundColor = EvaTheme.extendedColors.surfaceCard,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (status == AutomationTaskStatus.Running || status == AutomationTaskStatus.Planning) {
                        CircularProgressIndicator(
                            color = EvaTheme.extendedColors.secondaryCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Icon(
                            imageVector = when (status) {
                                AutomationTaskStatus.Completed -> Icons.Default.CheckCircle
                                AutomationTaskStatus.Failed -> Icons.Default.Error
                                AutomationTaskStatus.Cancelled -> Icons.Default.Warning
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = when (status) {
                                AutomationTaskStatus.Completed -> EvaTheme.extendedColors.secondaryCyan
                                AutomationTaskStatus.Failed -> Color(0xFFFF5252)
                                else -> EvaTheme.extendedColors.textSecondary
                            },
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
                    Text(
                        text = "Status: ${status.name}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = EvaTheme.extendedColors.secondaryCyan,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }

                if (status == AutomationTaskStatus.Running || status == AutomationTaskStatus.Planning) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("automation_btn_stop"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(R.string.automation_btn_stop),
                            tint = Color(0xFFFF5252),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))

            Text(
                text = "\"$prompt\"",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EvaTheme.extendedColors.textPrimary,
                ),
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))

            Text(
                text = currentDescription,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EvaTheme.extendedColors.textSecondary,
                ),
            )

            if (totalSteps > 0) {
                Spacer(modifier = Modifier.height(EvaTheme.spacing.small))
                Text(
                    text = "Step $completedSteps of $totalSteps completed",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EvaTheme.extendedColors.textDisabled,
                    ),
                )
            }
        }
    }
}

@Composable
private fun QuickCommandChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(EvaTheme.extendedColors.surfaceElevated)
            .border(1.dp, EvaTheme.extendedColors.borderSubtle, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EvaTheme.extendedColors.primaryPurple,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = EvaTheme.extendedColors.textPrimary,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

@Composable
private fun CapabilityRow(
    title: String,
    detail: String,
    isGranted: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = EvaTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = EvaTheme.extendedColors.textPrimary,
                ),
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = EvaTheme.extendedColors.textSecondary,
                ),
            )
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isGranted) EvaTheme.extendedColors.secondaryCyan else EvaTheme.extendedColors.textDisabled),
        )
    }
}

@Composable
private fun TaskHistoryCard(
    report: com.evai.rifatcoder.domain.automation.AutomationExecutionReport,
) {
    val dateStr = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(report.timestamp))

    EvaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = EvaTheme.extendedColors.surfaceElevated,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "\"${report.taskPrompt}\"",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = EvaTheme.extendedColors.textPrimary,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EvaTheme.extendedColors.textDisabled,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = report.status.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = when (report.status) {
                            AutomationTaskStatus.Completed -> EvaTheme.extendedColors.secondaryCyan
                            AutomationTaskStatus.Failed -> Color(0xFFFF5252)
                            else -> EvaTheme.extendedColors.textSecondary
                        },
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
                Text(
                    text = report.summary,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EvaTheme.extendedColors.textSecondary,
                    ),
                )
            }
        }
    }
}
