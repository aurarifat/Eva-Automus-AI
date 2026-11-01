package com.evai.rifatcoder.ui.voice

import com.evai.rifatcoder.data.voice.VoiceInfo
import com.evai.rifatcoder.data.voice.VoiceLanguagePreference
import com.evai.rifatcoder.data.voice.WakeWordFeasibilityReport
import com.evai.rifatcoder.data.voice.WakeWordStatus

/**
 * UI State for the Voice & Speech Settings screen.
 */
data class VoiceSettingsUiState(
    val sttLanguage: VoiceLanguagePreference = VoiceLanguagePreference.SYSTEM_DEFAULT,
    val isMicPermissionGranted: Boolean = false,
    val isSpeechRecognitionAvailable: Boolean = true,
    val ttsEnabled: Boolean = true,
    val ttsLanguage: VoiceLanguagePreference = VoiceLanguagePreference.ENGLISH,
    val installedVoices: List<VoiceInfo> = emptyList(),
    val selectedVoiceId: String? = null,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val isTestingVoice: Boolean = false,
    val testPhrase: String = "Hello! I am EVA, your intelligent companion.",
    val wakeWordStatus: WakeWordStatus = WakeWordStatus.RESTRICTED_BY_OS,
    val wakeWordEnabled: Boolean = false,
    val wakeWordPhrases: List<String> = listOf("Hey Eva", "Hey Eevva"),
    val feasibilityReport: WakeWordFeasibilityReport? = null,
    val showResetDialog: Boolean = false,
    val showFeasibilityDialog: Boolean = false,
    val userNotificationMessage: String? = null,
)
