package com.evai.rifatcoder.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evai.rifatcoder.data.voice.VoiceInfo
import com.evai.rifatcoder.data.voice.VoiceLanguagePreference
import com.evai.rifatcoder.data.voice.WakeWordStatus
import com.evai.rifatcoder.ui.components.EvaCard
import com.evai.rifatcoder.ui.components.EvaSectionHeader
import com.evai.rifatcoder.ui.theme.EvaTheme
import com.evai.rifatcoder.ui.voice.VoiceSettingsUiState
import com.evai.rifatcoder.ui.voice.VoiceSettingsViewModel
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoiceSettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Check microphone permission
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.setMicPermissionGranted(isGranted)
        }
    )

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setMicPermissionGranted(granted)
    }

    // Show notifications in snackbar
    LaunchedEffect(uiState.userNotificationMessage) {
        val msg = uiState.userNotificationMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotificationMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_voice_settings"),
        containerColor = EvaTheme.extendedColors.backgroundMain,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            VoiceSettingsTopBar(onNavigateBack = onNavigateBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EvaTheme.spacing.standard),
        ) {
            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            // Card A: Speech Recognition (STT)
            SpeechRecognitionCard(
                uiState = uiState,
                onLanguageSelected = { viewModel.setSttLanguage(it) },
                onRequestPermission = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // Card B: Speech Synthesis (TTS)
            SpeechSynthesisCard(
                uiState = uiState,
                onTtsEnabledChanged = { viewModel.setTtsEnabled(it) },
                onTtsLanguageSelected = { viewModel.setTtsLanguage(it) },
                onVoiceSelected = { viewModel.setSelectedVoiceId(it) },
                onRateChanged = { viewModel.setSpeechRate(it) },
                onPitchChanged = { viewModel.setSpeechPitch(it) },
                onTestVoice = { viewModel.testVoicePlayback() },
                onStopVoice = { viewModel.stopVoicePlayback() },
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // Card C: Wake-Word Feasibility & Disclosures
            WakeWordFeasibilityCard(
                uiState = uiState,
                onToggleWakeWord = { viewModel.toggleWakeWord(it) },
                onCheckFeasibility = { viewModel.runWakeWordFeasibilityAudit() }
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // Card D: Actions (Save & Reset)
            VoiceSettingsActionCard(
                onSave = { viewModel.saveSettings() },
                onResetClick = { viewModel.showResetConfirmation(true) }
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.large))
        }
    }

    // Reset Confirmation Dialog
    if (uiState.showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showResetConfirmation(false) },
            containerColor = EvaTheme.extendedColors.surfaceElevated,
            title = {
                Text(
                    text = "Reset Voice Settings?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EvaTheme.extendedColors.textPrimary
                    )
                )
            },
            text = {
                Text(
                    text = "This will restore speech recognition language, TTS rate, pitch, and voice parameters to default.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EvaTheme.extendedColors.textSecondary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetToDefaults() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EvaTheme.extendedColors.secondaryCyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("dialog_confirm_reset")
                ) {
                    Text("Reset Defaults")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showResetConfirmation(false) }) {
                    Text(
                        text = "Cancel",
                        color = EvaTheme.extendedColors.textSecondary
                    )
                }
            }
        )
    }

    // Feasibility Audit Dialog
    if (uiState.showFeasibilityDialog && uiState.feasibilityReport != null) {
        val report = uiState.feasibilityReport!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissFeasibilityDialog() },
            containerColor = EvaTheme.extendedColors.surfaceElevated,
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = EvaTheme.extendedColors.secondaryCyan
                )
            },
            title = {
                Text(
                    text = "Wake-Word Feasibility Audit",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EvaTheme.extendedColors.textPrimary
                    )
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Evaluation Status: ${report.status.displayName}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EvaTheme.extendedColors.primaryPurple
                        )
                    )
                    Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

                    Text(
                        text = "Target Phrases: ${report.targetPhrases.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = EvaTheme.extendedColors.textPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

                    Text(
                        text = "Android OS Policy Restrictions:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EvaTheme.extendedColors.secondaryCyan
                        )
                    )
                    Text(
                        text = report.backgroundOsRestrictions,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EvaTheme.extendedColors.textSecondary,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

                    Text(
                        text = "Battery Assessment:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EvaTheme.extendedColors.secondaryCyan
                        )
                    )
                    Text(
                        text = report.batteryImpactAssessment,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EvaTheme.extendedColors.textSecondary,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

                    Text(
                        text = "Privacy Guarantees:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EvaTheme.extendedColors.secondaryCyan
                        )
                    )
                    Text(
                        text = report.privacyNotice,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EvaTheme.extendedColors.textSecondary,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

                    Text(
                        text = "Recommended Path Forward:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EvaTheme.extendedColors.primaryPurple
                        )
                    )
                    Text(
                        text = report.recommendedArchitecture,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EvaTheme.extendedColors.textSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissFeasibilityDialog() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EvaTheme.extendedColors.primaryPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text("Understood")
                }
            }
        )
    }
}

@Composable
private fun VoiceSettingsTopBar(
    onNavigateBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = EvaTheme.spacing.small,
                vertical = EvaTheme.spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("button_back_voice_settings")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = EvaTheme.extendedColors.textPrimary
            )
        }
        Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
        Column {
            Text(
                text = "Voice & Speech Settings",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = EvaTheme.extendedColors.textPrimary
                )
            )
            Text(
                text = "Speech recognition, synthesis & wake-word engine",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EvaTheme.extendedColors.textSecondary
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeechRecognitionCard(
    uiState: VoiceSettingsUiState,
    onLanguageSelected: (VoiceLanguagePreference) -> Unit,
    onRequestPermission: () -> Unit,
) {
    EvaCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_speech_recognition"),
        backgroundColor = EvaTheme.extendedColors.surfaceCard
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = EvaTheme.extendedColors.primaryPurple,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
                Text(
                    text = "Speech Recognition (STT)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EvaTheme.extendedColors.textPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // Permission status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(EvaTheme.extendedColors.surfaceElevated)
                    .padding(EvaTheme.spacing.small)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.isMicPermissionGranted) Color(0xFF00E676)
                                else Color(0xFFFF5252)
                            )
                    )
                    Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
                    Text(
                        text = if (uiState.isMicPermissionGranted) "Microphone Authorized"
                        else "Microphone Permission Required",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EvaTheme.extendedColors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                if (!uiState.isMicPermissionGranted) {
                    TextButton(
                        onClick = onRequestPermission,
                        modifier = Modifier.testTag("button_request_mic_perm")
                    ) {
                        Text(
                            text = "Grant",
                            color = EvaTheme.extendedColors.secondaryCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            Text(
                text = "Recognition Language",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = EvaTheme.extendedColors.textSecondary
                )
            )
            Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                VoiceLanguagePreference.entries.forEach { pref ->
                    val isSelected = uiState.sttLanguage == pref
                    FilterChip(
                        label = pref.displayName,
                        isSelected = isSelected,
                        onClick = { onLanguageSelected(pref) },
                        testTag = "stt_lang_${pref.id}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            Text(
                text = if (uiState.isSpeechRecognitionAvailable)
                    "• Native Android SpeechRecognizer engine is operational."
                else
                    "• Warning: Speech recognition is unavailable on this device.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (uiState.isSpeechRecognitionAvailable)
                        EvaTheme.extendedColors.textSecondary
                    else Color(0xFFFFAB40),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeechSynthesisCard(
    uiState: VoiceSettingsUiState,
    onTtsEnabledChanged: (Boolean) -> Unit,
    onTtsLanguageSelected: (VoiceLanguagePreference) -> Unit,
    onVoiceSelected: (String?) -> Unit,
    onRateChanged: (Float) -> Unit,
    onPitchChanged: (Float) -> Unit,
    onTestVoice: () -> Unit,
    onStopVoice: () -> Unit,
) {
    var voiceDropdownExpanded by remember { mutableStateOf(false) }

    EvaCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_speech_synthesis"),
        backgroundColor = EvaTheme.extendedColors.surfaceCard
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = EvaTheme.extendedColors.secondaryCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
                    Text(
                        text = "Speech Synthesis (TTS)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EvaTheme.extendedColors.textPrimary
                        )
                    )
                }

                Switch(
                    checked = uiState.ttsEnabled,
                    onCheckedChange = onTtsEnabledChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EvaTheme.extendedColors.primaryPurple,
                        uncheckedTrackColor = EvaTheme.extendedColors.surfaceElevated
                    ),
                    modifier = Modifier.testTag("switch_tts_enabled")
                )
            }

            AnimatedVisibility(visible = uiState.ttsEnabled) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

                    Text(
                        text = "Spoken Output Language",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EvaTheme.extendedColors.textSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VoiceLanguagePreference.entries.forEach { pref ->
                            val isSelected = uiState.ttsLanguage == pref
                            FilterChip(
                                label = pref.displayName,
                                isSelected = isSelected,
                                onClick = { onTtsLanguageSelected(pref) },
                                testTag = "tts_lang_${pref.id}"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

                    // Voice Selection (if installed voices available)
                    if (uiState.installedVoices.isNotEmpty()) {
                        Text(
                            text = "Installed Voice Model",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = EvaTheme.extendedColors.textSecondary
                            )
                        )
                        Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(EvaTheme.extendedColors.surfaceElevated)
                                .clickable { voiceDropdownExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("dropdown_voice_selector")
                        ) {
                            val selectedName = uiState.installedVoices
                                .firstOrNull { it.id == uiState.selectedVoiceId }?.name
                                ?: "System Default Engine Voice"
                            Text(
                                text = selectedName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = EvaTheme.extendedColors.textPrimary
                                )
                            )

                            DropdownMenu(
                                expanded = voiceDropdownExpanded,
                                onDismissRequest = { voiceDropdownExpanded = false },
                                modifier = Modifier.background(EvaTheme.extendedColors.surfaceElevated)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("System Default", color = Color.White) },
                                    onClick = {
                                        onVoiceSelected(null)
                                        voiceDropdownExpanded = false
                                    }
                                )
                                uiState.installedVoices.take(15).forEach { voice ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${voice.name} (${voice.localeTag})",
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        },
                                        onClick = {
                                            onVoiceSelected(voice.id)
                                            voiceDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))
                    }

                    // Speech Rate Slider (0.75x to 1.50x)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Speech Rate",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = EvaTheme.extendedColors.textSecondary
                            )
                        )
                        Text(
                            text = String.format(Locale.US, "%.2fx", uiState.speechRate),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EvaTheme.extendedColors.secondaryCyan
                            )
                        )
                    }

                    Slider(
                        value = uiState.speechRate,
                        onValueChange = onRateChanged,
                        valueRange = 0.75f..1.50f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = EvaTheme.extendedColors.secondaryCyan,
                            activeTrackColor = EvaTheme.extendedColors.secondaryCyan,
                            inactiveTrackColor = EvaTheme.extendedColors.surfaceElevated
                        ),
                        modifier = Modifier.testTag("slider_speech_rate")
                    )

                    Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

                    // Pitch Slider (0.80x to 1.20x)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voice Pitch",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = EvaTheme.extendedColors.textSecondary
                            )
                        )
                        Text(
                            text = String.format(Locale.US, "%.2fx", uiState.speechPitch),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EvaTheme.extendedColors.primaryPurple
                            )
                        )
                    }

                    Slider(
                        value = uiState.speechPitch,
                        onValueChange = onPitchChanged,
                        valueRange = 0.80f..1.20f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = EvaTheme.extendedColors.primaryPurple,
                            activeTrackColor = EvaTheme.extendedColors.primaryPurple,
                            inactiveTrackColor = EvaTheme.extendedColors.surfaceElevated
                        ),
                        modifier = Modifier.testTag("slider_speech_pitch")
                    )

                    Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

                    // Test Voice Playback Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onTestVoice,
                            enabled = !uiState.isTestingVoice,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EvaTheme.extendedColors.primaryPurple,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_test_voice")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (uiState.isTestingVoice) "Playing…" else "Test Voice")
                        }

                        if (uiState.isTestingVoice) {
                            OutlinedButton(
                                onClick = onStopVoice,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFF5252)
                                ),
                                modifier = Modifier.testTag("button_stop_voice_test")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WakeWordFeasibilityCard(
    uiState: VoiceSettingsUiState,
    onToggleWakeWord: (Boolean) -> Unit,
    onCheckFeasibility: () -> Unit,
) {
    EvaCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_wake_word_feasibility"),
        backgroundColor = EvaTheme.extendedColors.surfaceCard
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFFFFAB40),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
                    Text(
                        text = "Wake-Word Detection",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EvaTheme.extendedColors.textPrimary
                        )
                    )
                }

                // Status Badge
                StatusBadge(status = uiState.wakeWordStatus)
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            Text(
                text = "Target Wake Phrases",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = EvaTheme.extendedColors.textSecondary
                )
            )
            Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.wakeWordPhrases.forEach { phrase ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(EvaTheme.extendedColors.surfaceElevated)
                            .border(
                                1.dp,
                                EvaTheme.extendedColors.primaryPurple.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "\"$phrase\"",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EvaTheme.extendedColors.textPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // Honest Technical Disclosure Summary Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(EvaTheme.extendedColors.surfaceElevated)
                    .padding(EvaTheme.spacing.standard)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFAB40),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Outcome B: Android Background Execution Limits",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFAB40)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Android 11+ restricts background microphone capture without foreground authorization. Continuous listening also incurs high battery drain without dedicated DSP hardware.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EvaTheme.extendedColors.textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // Toggle (Honest: explains restriction when toggled)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Listen for \"Hey Eva\"",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EvaTheme.extendedColors.textPrimary
                        )
                    )
                    Text(
                        text = "Background hotword listening (Restricted by OS policy)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EvaTheme.extendedColors.textSecondary,
                            fontSize = 11.sp
                        )
                    )
                }

                Switch(
                    checked = uiState.wakeWordEnabled,
                    onCheckedChange = { onToggleWakeWord(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EvaTheme.extendedColors.primaryPurple,
                        uncheckedTrackColor = EvaTheme.extendedColors.surfaceElevated
                    ),
                    modifier = Modifier.testTag("switch_wake_word_enabled")
                )
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            // View Audit Button
            OutlinedButton(
                onClick = onCheckFeasibility,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = EvaTheme.extendedColors.secondaryCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button_check_feasibility")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("View Detailed Technical Audit")
            }
        }
    }
}

@Composable
private fun StatusBadge(status: WakeWordStatus) {
    val (bgColor, textColor) = when (status) {
        WakeWordStatus.ACTIVE -> Pair(Color(0xFF00E676).copy(alpha = 0.2f), Color(0xFF00E676))
        WakeWordStatus.RESTRICTED_BY_OS -> Pair(Color(0xFFFFAB40).copy(alpha = 0.2f), Color(0xFFFFAB40))
        WakeWordStatus.REQUIRES_DEVICE_SETUP -> Pair(Color(0xFFFFAB40).copy(alpha = 0.2f), Color(0xFFFFAB40))
        WakeWordStatus.NOT_CONFIGURED, WakeWordStatus.UNAVAILABLE -> Pair(Color(0xFFFF5252).copy(alpha = 0.2f), Color(0xFFFF5252))
        WakeWordStatus.FEASIBILITY_CHECK_REQUIRED -> Pair(Color(0xFF8A7FFF).copy(alpha = 0.2f), Color(0xFF8A7FFF))
        WakeWordStatus.DISABLED -> Pair(Color.Gray.copy(alpha = 0.2f), Color.LightGray)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.displayName.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 9.sp
            )
        )
    }
}

@Composable
private fun VoiceSettingsActionCard(
    onSave: () -> Unit,
    onResetClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onResetClick,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = EvaTheme.extendedColors.textSecondary
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("button_reset_voice_settings")
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset")
        }

        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(
                containerColor = EvaTheme.extendedColors.primaryPurple,
                contentColor = Color.White
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("button_save_voice_settings")
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Settings")
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) EvaTheme.extendedColors.primaryPurple.copy(alpha = 0.25f)
                else EvaTheme.extendedColors.surfaceElevated
            )
            .border(
                1.dp,
                if (isSelected) EvaTheme.extendedColors.primaryPurple
                else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = EvaTheme.extendedColors.secondaryCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) EvaTheme.extendedColors.textPrimary
                    else EvaTheme.extendedColors.textSecondary
                )
            )
        }
    }
}
