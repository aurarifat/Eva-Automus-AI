package com.evai.rifatcoder.ui.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evai.rifatcoder.data.voice.DefaultWakeWordDetector
import com.evai.rifatcoder.data.voice.SpeechRecognizerManager
import com.evai.rifatcoder.data.voice.TextToSpeechManager
import com.evai.rifatcoder.data.voice.TtsCallback
import com.evai.rifatcoder.data.voice.VoiceLanguagePreference
import com.evai.rifatcoder.data.voice.VoicePreferenceState
import com.evai.rifatcoder.data.voice.VoicePreferencesRepository
import com.evai.rifatcoder.data.voice.WakeWordDetector
import com.evai.rifatcoder.data.voice.WakeWordStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel managing Voice & Speech configuration, TTS preview, and wake-word feasibility.
 */
open class VoiceSettingsViewModel @JvmOverloads constructor(
    application: Application,
    private val preferencesRepository: VoicePreferencesRepository = VoicePreferencesRepository(application),
    private val ttsManager: TextToSpeechManager = TextToSpeechManager(application),
    private val speechManager: SpeechRecognizerManager = SpeechRecognizerManager(application),
    private val wakeWordDetector: WakeWordDetector = DefaultWakeWordDetector(application),
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VoiceSettingsUiState())
    val uiState: StateFlow<VoiceSettingsUiState> = _uiState.asStateFlow()

    private var currentTestUtteranceId: String? = null

    init {
        setupTtsListener()
        loadInitialState()
    }

    private fun loadInitialState() {
        val saved = preferencesRepository.getPreferences()
        val speechAvailable = speechManager.isRecognitionAvailable()
        val voices = ttsManager.getAvailableVoices()
        val feasibility = wakeWordDetector.checkFeasibility()

        _uiState.update { current ->
            current.copy(
                sttLanguage = saved.sttLanguage,
                ttsEnabled = saved.ttsEnabled,
                ttsLanguage = saved.ttsLanguage,
                selectedVoiceId = saved.ttsVoiceId,
                speechRate = saved.speechRate,
                speechPitch = saved.speechPitch,
                isSpeechRecognitionAvailable = speechAvailable,
                installedVoices = voices,
                wakeWordStatus = feasibility.status,
                wakeWordEnabled = saved.wakeWordEnabled && feasibility.isFeasibleOnCurrentDevice,
                wakeWordPhrases = feasibility.targetPhrases,
                feasibilityReport = feasibility,
            )
        }

        // Apply saved rates to TTS engine
        ttsManager.setSpeechRate(saved.speechRate)
        ttsManager.setPitch(saved.speechPitch)
        ttsManager.setVoice(saved.ttsVoiceId)
    }

    private fun setupTtsListener() {
        ttsManager.setCallback(object : TtsCallback {
            override fun onInitSuccess() {
                val voices = ttsManager.getAvailableVoices()
                _uiState.update { current ->
                    current.copy(installedVoices = voices)
                }
            }

            override fun onInitError(message: String) {
                _uiState.update { current ->
                    current.copy(
                        isTestingVoice = false,
                        userNotificationMessage = "TTS initialization error: $message"
                    )
                }
            }

            override fun onSpeechStarted(utteranceId: String) {
                if (utteranceId == currentTestUtteranceId) {
                    _uiState.update { current ->
                        current.copy(isTestingVoice = true)
                    }
                }
            }

            override fun onSpeechFinished(utteranceId: String) {
                if (utteranceId == currentTestUtteranceId) {
                    _uiState.update { current ->
                        current.copy(isTestingVoice = false)
                    }
                }
            }

            override fun onSpeechError(utteranceId: String, errorCode: Int) {
                if (utteranceId == currentTestUtteranceId) {
                    _uiState.update { current ->
                        current.copy(
                            isTestingVoice = false,
                            userNotificationMessage = "Voice playback failed (code $errorCode)"
                        )
                    }
                }
            }
        })
    }

    fun setSttLanguage(pref: VoiceLanguagePreference) {
        _uiState.update { it.copy(sttLanguage = pref) }
    }

    fun setMicPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(isMicPermissionGranted = granted) }
    }

    fun setTtsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(ttsEnabled = enabled) }
        if (!enabled && _uiState.value.isTestingVoice) {
            stopVoicePlayback()
        }
    }

    fun setTtsLanguage(pref: VoiceLanguagePreference) {
        _uiState.update { it.copy(ttsLanguage = pref) }
    }

    fun setSelectedVoiceId(voiceId: String?) {
        _uiState.update { it.copy(selectedVoiceId = voiceId) }
        ttsManager.setVoice(voiceId)
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(
            VoicePreferenceState.MIN_SPEECH_RATE,
            VoicePreferenceState.MAX_SPEECH_RATE
        )
        _uiState.update { it.copy(speechRate = clamped) }
        ttsManager.setSpeechRate(clamped)
    }

    fun setSpeechPitch(pitch: Float) {
        val clamped = pitch.coerceIn(
            VoicePreferenceState.MIN_SPEECH_PITCH,
            VoicePreferenceState.MAX_SPEECH_PITCH
        )
        _uiState.update { it.copy(speechPitch = clamped) }
        ttsManager.setPitch(clamped)
    }

    fun testVoicePlayback() {
        val currentState = _uiState.value
        if (!currentState.ttsEnabled) {
            _uiState.update { it.copy(userNotificationMessage = "Enable Voice Output (TTS) to test playback.") }
            return
        }

        val targetLang = currentState.ttsLanguage.toVoiceLanguage()
        val utteranceId = "test_voice_" + UUID.randomUUID().toString()
        currentTestUtteranceId = utteranceId

        _uiState.update { it.copy(isTestingVoice = true) }

        val started = ttsManager.speak(
            text = currentState.testPhrase,
            utteranceId = utteranceId,
            language = targetLang,
            rate = currentState.speechRate,
            pitch = currentState.speechPitch,
            voiceId = currentState.selectedVoiceId
        )

        if (!started) {
            _uiState.update {
                it.copy(
                    isTestingVoice = false,
                    userNotificationMessage = "Text-to-Speech engine is not ready or busy."
                )
            }
        }
    }

    fun stopVoicePlayback() {
        ttsManager.stop()
        _uiState.update { it.copy(isTestingVoice = false) }
    }

    fun runWakeWordFeasibilityAudit() {
        val report = wakeWordDetector.checkFeasibility()
        _uiState.update {
            it.copy(
                feasibilityReport = report,
                wakeWordStatus = report.status,
                showFeasibilityDialog = true,
            )
        }
    }

    fun toggleWakeWord(attemptEnable: Boolean) {
        if (attemptEnable) {
            val report = wakeWordDetector.checkFeasibility()
            _uiState.update {
                it.copy(
                    wakeWordEnabled = false,
                    feasibilityReport = report,
                    wakeWordStatus = report.status,
                    showFeasibilityDialog = true,
                    userNotificationMessage = "Wake-word restricted by Android OS background policy."
                )
            }
        } else {
            wakeWordDetector.stopListening()
            _uiState.update {
                it.copy(
                    wakeWordEnabled = false,
                    wakeWordStatus = WakeWordStatus.DISABLED
                )
            }
            preferencesRepository.setWakeWordEnabled(false)
        }
    }

    fun dismissFeasibilityDialog() {
        _uiState.update { it.copy(showFeasibilityDialog = false) }
    }

    fun showResetConfirmation(show: Boolean) {
        _uiState.update { it.copy(showResetDialog = show) }
    }

    fun resetToDefaults() {
        val defaults = preferencesRepository.resetToDefaults()
        ttsManager.setSpeechRate(defaults.speechRate)
        ttsManager.setPitch(defaults.speechPitch)
        ttsManager.setVoice(defaults.ttsVoiceId)

        _uiState.update { current ->
            current.copy(
                sttLanguage = defaults.sttLanguage,
                ttsEnabled = defaults.ttsEnabled,
                ttsLanguage = defaults.ttsLanguage,
                selectedVoiceId = defaults.ttsVoiceId,
                speechRate = defaults.speechRate,
                speechPitch = defaults.speechPitch,
                wakeWordEnabled = false,
                wakeWordStatus = WakeWordStatus.RESTRICTED_BY_OS,
                showResetDialog = false,
                userNotificationMessage = "Voice settings restored to default."
            )
        }
    }

    fun saveSettings() {
        val currentState = _uiState.value
        val toSave = VoicePreferenceState(
            sttLanguage = currentState.sttLanguage,
            ttsEnabled = currentState.ttsEnabled,
            ttsLanguage = currentState.ttsLanguage,
            ttsVoiceId = currentState.selectedVoiceId,
            speechRate = currentState.speechRate,
            speechPitch = currentState.speechPitch,
            wakeWordEnabled = currentState.wakeWordEnabled,
        )

        preferencesRepository.saveAll(toSave)
        ttsManager.setSpeechRate(currentState.speechRate)
        ttsManager.setPitch(currentState.speechPitch)
        ttsManager.setVoice(currentState.selectedVoiceId)

        _uiState.update {
            it.copy(userNotificationMessage = "Voice settings saved successfully.")
        }
    }

    fun clearNotificationMessage() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopVoicePlayback()
    }
}
