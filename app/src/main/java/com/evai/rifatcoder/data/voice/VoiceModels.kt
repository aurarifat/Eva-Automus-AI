package com.evai.rifatcoder.data.voice

/**
 * Legacy state enum for EVA AI Voice Assistant.
 * Maps seamlessly to and from CentralVoiceState.
 */
enum class VoiceState {
    READY,
    REQUESTING_PERMISSION,
    LISTENING,
    RECOGNIZING,
    THINKING,
    SPEAKING,
    ERROR;

    fun toCentralVoiceState(): CentralVoiceState = when (this) {
        READY -> CentralVoiceState.IDLE
        REQUESTING_PERMISSION -> CentralVoiceState.STARTING
        LISTENING -> CentralVoiceState.LISTENING
        RECOGNIZING, THINKING -> CentralVoiceState.PROCESSING
        SPEAKING -> CentralVoiceState.SPEAKING
        ERROR -> CentralVoiceState.ERROR
    }
}

/**
 * Supported speech and synthesis languages.
 */
enum class VoiceLanguage(
    val code: String,
    val localeTag: String,
    val displayName: String,
) {
    ENGLISH("en", "en-US", "English"),
    BENGALI("bn", "bn-BD", "বাংলা"),
}

/**
 * Immutable UI State for VoiceScreen.
 */
data class VoiceUiState(
    val voiceState: VoiceState = VoiceState.READY,
    val centralState: CentralVoiceState = CentralVoiceState.IDLE,
    val activeLanguage: VoiceLanguage = VoiceLanguage.ENGLISH,
    val recognizedText: String = "",
    val interimSpeechText: String = "",
    val assistantResponseText: String = "",
    val isSpeechAvailable: Boolean = true,
    val isTtsReady: Boolean = false,
    val soundLevel: Float = 0f,
    val errorMessage: String? = null,
    val conversationId: String = "",
    val isPermissionGranted: Boolean = false,
    val isTtsSpeaking: Boolean = false,
    val isIncognito: Boolean = false,
)
