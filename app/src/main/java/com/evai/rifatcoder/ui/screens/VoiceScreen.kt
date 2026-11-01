package com.evai.rifatcoder.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evai.rifatcoder.R
import com.evai.rifatcoder.data.voice.CentralVoiceState
import com.evai.rifatcoder.data.voice.VoiceLanguage
import com.evai.rifatcoder.data.voice.VoiceState
import com.evai.rifatcoder.data.voice.VoiceUiState
import com.evai.rifatcoder.ui.components.EvaAnimatedOrb
import com.evai.rifatcoder.ui.components.EvaCard
import com.evai.rifatcoder.ui.components.EvaStatusIndicator
import com.evai.rifatcoder.ui.components.EvaStatusLevel
import com.evai.rifatcoder.ui.theme.EvaTheme
import com.evai.rifatcoder.ui.voice.VoiceViewModel

@Composable
fun VoiceScreen(
    modifier: Modifier = Modifier,
    viewModel: VoiceViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setPermissionGranted(isGranted)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setPermissionGranted(granted)
    }

    LaunchedEffect(uiState.voiceState) {
        if (uiState.voiceState == VoiceState.REQUESTING_PERMISSION) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EvaTheme.extendedColors.backgroundMain)
            .testTag("screen_voice"),
    ) {
        // TOP APP BAR
        VoiceTopAppBar(
            activeLanguage = uiState.activeLanguage,
            onToggleLanguage = { viewModel.toggleLanguage() },
            onNavigateBack = onNavigateBack,
            onNavigateToSettings = onNavigateToSettings,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EvaTheme.spacing.standard),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            // ASSISTANT STATUS BADGE
            VoiceStatusBadge(voiceState = uiState.voiceState)

            Spacer(modifier = Modifier.height(EvaTheme.spacing.medium))

            // CENTRAL INTERACTIVE VOICE ORB
            InteractiveVoiceOrb(
                uiState = uiState,
                onClick = {
                    if (!uiState.isPermissionGranted) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.onMicTapped()
                    }
                },
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            Text(
                text = resolveOrbPrompt(uiState.centralState),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = EvaTheme.extendedColors.textSecondary,
                    textAlign = TextAlign.Center,
                ),
            )

            // ERROR BANNER
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                uiState.errorMessage?.let { errorText ->
                    Spacer(modifier = Modifier.height(EvaTheme.spacing.small))
                    VoiceErrorCard(
                        message = errorText,
                        onDismiss = { viewModel.dismissError() },
                    )
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.medium))

            // TRANSCRIPTION CARD
            TranscriptionCard(
                interimText = uiState.interimSpeechText,
                recognizedText = uiState.recognizedText,
                voiceState = uiState.voiceState,
                onTextChanged = { viewModel.updateTranscriptionText(it) },
                onSend = { viewModel.submitManualPrompt() },
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.medium))

            // AI RESPONSE CARD
            AiResponseCard(
                responseText = uiState.assistantResponseText,
                isSpeaking = uiState.isTtsSpeaking,
                onReplay = { viewModel.replayResponse() },
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.large))

            // BOTTOM CONTROL BAR
            VoiceControlsBar(
                uiState = uiState,
                onMicClick = {
                    if (!uiState.isPermissionGranted) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.onMicTapped()
                    }
                },
                onStopClick = { viewModel.stopActiveSession() },
                onToggleLanguage = { viewModel.toggleLanguage() },
                onChatModeClick = onNavigateToChat,
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.large))
        }
    }
}

@Composable
private fun VoiceTopAppBar(
    activeLanguage: VoiceLanguage,
    onToggleLanguage: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = EvaTheme.spacing.standard,
                vertical = EvaTheme.spacing.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("voice_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back_button),
                    tint = EvaTheme.extendedColors.textPrimary,
                )
            }
            Spacer(modifier = Modifier.width(EvaTheme.spacing.extraSmall))
            Column {
                Text(
                    text = stringResource(R.string.title_voice),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = EvaTheme.extendedColors.textPrimary,
                    ),
                )
                Text(
                    text = stringResource(R.string.subtitle_voice),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EvaTheme.extendedColors.textSecondary,
                    ),
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Language Badge Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(EvaTheme.extendedColors.surfaceElevated)
                    .border(
                        width = 1.dp,
                        color = EvaTheme.extendedColors.primaryPurple.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable(
                        role = Role.Button,
                        onClick = onToggleLanguage,
                    )
                    .semantics { contentDescription = "Switch Language" }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("voice_language_toggle_pill"),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = EvaTheme.extendedColors.secondaryCyan,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activeLanguage.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EvaTheme.extendedColors.textPrimary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.width(EvaTheme.spacing.small))

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.testTag("voice_settings_button"),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.cd_settings_button),
                    tint = EvaTheme.extendedColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun VoiceStatusBadge(voiceState: VoiceState) {
    val (statusLevel, statusLabel) = when (voiceState) {
        VoiceState.READY -> Pair(EvaStatusLevel.Available, stringResource(R.string.voice_status_ready))
        VoiceState.REQUESTING_PERMISSION -> Pair(EvaStatusLevel.InProgress, stringResource(R.string.voice_status_requesting_permission))
        VoiceState.LISTENING -> Pair(EvaStatusLevel.InProgress, stringResource(R.string.voice_status_listening))
        VoiceState.RECOGNIZING -> Pair(EvaStatusLevel.InProgress, stringResource(R.string.voice_status_recognizing))
        VoiceState.THINKING -> Pair(EvaStatusLevel.InProgress, stringResource(R.string.voice_status_thinking))
        VoiceState.SPEAKING -> Pair(EvaStatusLevel.Available, stringResource(R.string.voice_status_speaking))
        VoiceState.ERROR -> Pair(EvaStatusLevel.Error, stringResource(R.string.voice_status_error))
    }

    EvaStatusIndicator(
        status = statusLevel,
        customLabel = statusLabel,
    )
}

@Composable
private fun InteractiveVoiceOrb(
    uiState: VoiceUiState,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        EvaAnimatedOrb(
            size = 200.dp,
            voiceState = uiState.centralState,
            soundLevel = uiState.soundLevel,
            onClick = onClick,
            testTag = "voice_central_interactive_orb",
        )
    }
}

@Composable
private fun TranscriptionCard(
    interimText: String,
    recognizedText: String,
    voiceState: VoiceState,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    val displayedText = if (voiceState == VoiceState.LISTENING && interimText.isNotBlank()) {
        interimText
    } else {
        recognizedText
    }

    EvaCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_transcription_card"),
        backgroundColor = EvaTheme.extendedColors.surfaceCard,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.voice_transcription_label),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = EvaTheme.extendedColors.secondaryCyan,
                    ),
                )
                if (displayedText.isNotBlank()) {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("voice_btn_send_prompt"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = stringResource(R.string.voice_action_send),
                            tint = EvaTheme.extendedColors.primaryPurple,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            if (voiceState == VoiceState.LISTENING && interimText.isNotBlank()) {
                Text(
                    text = "“$interimText”",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = EvaTheme.extendedColors.textPrimary,
                        fontStyle = FontStyle.Italic,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (recognizedText.isNotBlank()) {
                OutlinedTextField(
                    value = recognizedText,
                    onValueChange = onTextChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_transcription_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EvaTheme.extendedColors.primaryPurple,
                        unfocusedBorderColor = EvaTheme.extendedColors.borderSubtle,
                        focusedTextColor = EvaTheme.extendedColors.textPrimary,
                        unfocusedTextColor = EvaTheme.extendedColors.textPrimary,
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                )
            } else {
                Text(
                    text = stringResource(R.string.voice_transcription_empty),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EvaTheme.extendedColors.textDisabled,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AiResponseCard(
    responseText: String,
    isSpeaking: Boolean,
    onReplay: () -> Unit,
) {
    EvaCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_response_card"),
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
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = if (isSpeaking) EvaTheme.extendedColors.secondaryCyan else EvaTheme.extendedColors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
                    Text(
                        text = stringResource(R.string.voice_response_label),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EvaTheme.extendedColors.primaryPurple,
                        ),
                    )
                }

                if (responseText.isNotBlank()) {
                    IconButton(
                        onClick = onReplay,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("voice_replay_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = stringResource(R.string.voice_cd_replay),
                            tint = EvaTheme.extendedColors.primaryPurple,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            if (responseText.isNotBlank()) {
                Text(
                    text = responseText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EvaTheme.extendedColors.textPrimary,
                        lineHeight = 22.sp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = stringResource(R.string.voice_response_empty),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EvaTheme.extendedColors.textDisabled,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun VoiceControlsBar(
    uiState: VoiceUiState,
    onMicClick: () -> Unit,
    onStopClick: () -> Unit,
    onToggleLanguage: () -> Unit,
    onChatModeClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_controls_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        // Chat Mode Switcher Button
        IconButton(
            onClick = onChatModeClick,
            modifier = Modifier
                .size(48.dp)
                .background(EvaTheme.extendedColors.surfaceCard, CircleShape)
                .testTag("voice_btn_switch_chat"),
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubble,
                contentDescription = stringResource(R.string.voice_action_switch_to_chat),
                tint = EvaTheme.extendedColors.textSecondary,
            )
        }

        // Primary Mic Floating Action Button
        val isListeningOrSpeaking = uiState.voiceState == VoiceState.LISTENING ||
                uiState.voiceState == VoiceState.SPEAKING ||
                uiState.voiceState == VoiceState.THINKING

        FloatingActionButton(
            onClick = onMicClick,
            modifier = Modifier
                .size(64.dp)
                .testTag("voice_main_mic_fab"),
            shape = CircleShape,
            containerColor = if (uiState.voiceState == VoiceState.LISTENING) {
                EvaTheme.extendedColors.secondaryCyan
            } else {
                EvaTheme.extendedColors.primaryPurple
            },
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(8.dp),
        ) {
            Icon(
                imageVector = if (uiState.voiceState == VoiceState.LISTENING) {
                    Icons.Default.Stop
                } else {
                    Icons.Default.Mic
                },
                contentDescription = stringResource(R.string.voice_cd_mic),
                modifier = Modifier.size(32.dp),
            )
        }

        // Stop / Cancel Button (Enabled when active)
        IconButton(
            onClick = onStopClick,
            enabled = isListeningOrSpeaking,
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isListeningOrSpeaking) EvaTheme.extendedColors.surfaceElevated else EvaTheme.extendedColors.surfaceCard.copy(alpha = 0.5f),
                    CircleShape
                )
                .testTag("voice_btn_stop"),
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = stringResource(R.string.voice_cd_stop),
                tint = if (isListeningOrSpeaking) Color(0xFFEF5350) else EvaTheme.extendedColors.textDisabled,
            )
        }
    }
}

@Composable
private fun VoiceErrorCard(
    message: String,
    onDismiss: () -> Unit,
) {
    EvaCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_error_card"),
        backgroundColor = Color(0xFF2A1519),
        borderColor = Color(0xFFEF5350).copy(alpha = 0.4f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFFFCDD2),
                    ),
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = "Dismiss",
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun resolveOrbPrompt(state: CentralVoiceState): String {
    return when (state) {
        CentralVoiceState.IDLE -> "Tap orb to speak with EVA"
        CentralVoiceState.STARTING -> "Preparing microphone…"
        CentralVoiceState.LISTENING -> "Listening… Speak clearly"
        CentralVoiceState.PROCESSING -> "Thinking…"
        CentralVoiceState.SPEAKING -> "Speaking answer aloud (tap to stop)"
        CentralVoiceState.EXECUTING_TASK -> "Executing accessibility task…"
        CentralVoiceState.STOPPING -> "Stopping…"
        CentralVoiceState.ERROR -> "Voice session interrupted. Tap to retry."
    }
}
