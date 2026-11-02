package com.evai.rifatcoder.ui.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evai.rifatcoder.data.database.EvaDatabase
import com.evai.rifatcoder.data.repository.ConversationRepository
import com.evai.rifatcoder.data.repository.RealConversationRepository
import com.evai.rifatcoder.data.security.SecureConfigStorage
import com.evai.rifatcoder.data.voice.CentralVoiceState
import com.evai.rifatcoder.data.voice.CentralVoiceStateManager
import com.evai.rifatcoder.data.voice.SpeechRecognitionCallback
import com.evai.rifatcoder.data.voice.SpeechRecognizerManager
import com.evai.rifatcoder.data.voice.TextToSpeechManager
import com.evai.rifatcoder.data.voice.TtsCallback
import com.evai.rifatcoder.data.voice.VoiceLanguage
import com.evai.rifatcoder.data.voice.VoiceLanguagePreference
import com.evai.rifatcoder.data.voice.VoicePreferencesRepository
import com.evai.rifatcoder.data.voice.VoiceState
import com.evai.rifatcoder.data.voice.VoiceUiState
import com.evai.rifatcoder.domain.chat.AiChatEngine
import com.evai.rifatcoder.domain.chat.ChatMessage
import com.evai.rifatcoder.domain.chat.ChatRole
import com.evai.rifatcoder.domain.chat.MessageStatus
import com.evai.rifatcoder.domain.chat.RealAiChatEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Intelligent Voice Assistant ViewModel for EVA AI (Phase 13).
 * Bridges SpeechRecognizerManager and TextToSpeechManager with CentralVoiceStateManager,
 * AiChatEngine, ConversationRepository, and AutomationCoordinator.
 */
class VoiceViewModel @JvmOverloads constructor(
    application: Application,
    private val speechManager: SpeechRecognizerManager = SpeechRecognizerManager(application),
    private val ttsManager: TextToSpeechManager = TextToSpeechManager(application),
    private val chatEngine: AiChatEngine = RealAiChatEngine(SecureConfigStorage(application)),
    private val conversationRepository: ConversationRepository = RealConversationRepository(
        EvaDatabase.getInstance(application)
    ),
    private val preferencesRepository: VoicePreferencesRepository = VoicePreferencesRepository(application),
    private val memoryRepository: com.evai.rifatcoder.data.repository.MemoryRepository =
        com.evai.rifatcoder.data.repository.RealMemoryRepository(
            EvaDatabase.getInstance(application),
            com.evai.rifatcoder.data.memory.MemoryPreferencesRepository(application)
        ),
    private val automationCoordinator: com.evai.rifatcoder.ui.automation.AutomationCoordinator =
        com.evai.rifatcoder.ui.automation.AutomationCoordinator(application),
    private val centralStateManager: CentralVoiceStateManager = CentralVoiceStateManager,
    private val contextBuilder: com.evai.rifatcoder.domain.chat.ChatContextBuilder =
        com.evai.rifatcoder.domain.chat.ChatContextBuilder(),
    private val aiToolRegistry: com.evai.rifatcoder.domain.tool.AiToolRegistry =
        com.evai.rifatcoder.domain.tool.AiToolRegistry(application),
) : AndroidViewModel(application) {

    private val _uiState: MutableStateFlow<VoiceUiState> = MutableStateFlow(
        VoiceUiState(
            isSpeechAvailable = speechManager.isRecognitionAvailable(),
            conversationId = UUID.randomUUID().toString(),
            centralState = centralStateManager.currentState,
            voiceState = centralStateManager.currentState.toLegacyVoiceState(),
        )
    )
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var currentSessionId: String = UUID.randomUUID().toString()
    private var activeAiJob: Job? = null

    init {
        setupSpeechCallbacks()
        setupTtsCallbacks()
        loadVoicePreferences()

        // Synchronize CentralVoiceStateManager with UI State
        viewModelScope.launch {
            centralStateManager.state.collect { centralState ->
                _uiState.update { current ->
                    current.copy(
                        centralState = centralState,
                        voiceState = centralState.toLegacyVoiceState(),
                    )
                }
            }
        }

        viewModelScope.launch {
            centralStateManager.soundLevel.collect { soundLevel ->
                _uiState.update { current ->
                    current.copy(soundLevel = soundLevel)
                }
            }
        }
    }

    private fun loadVoicePreferences() {
        val saved = preferencesRepository.getPreferences()
        val initialLang = if (saved.sttLanguage != VoiceLanguagePreference.SYSTEM_DEFAULT) {
            saved.sttLanguage.toVoiceLanguage()
        } else {
            VoiceLanguage.ENGLISH
        }
        _uiState.update { current ->
            current.copy(activeLanguage = initialLang)
        }
        ttsManager.setSpeechRate(saved.speechRate)
        ttsManager.setPitch(saved.speechPitch)
        ttsManager.setVoice(saved.ttsVoiceId)
    }

    private fun setupSpeechCallbacks() {
        speechManager.setCallback(object : SpeechRecognitionCallback {
            override fun onReady() {
                // Speech recognition is genuinely active and ready for microphone input
                centralStateManager.notifyReadyForSpeech()
                _uiState.update { current ->
                    current.copy(
                        voiceState = VoiceState.LISTENING,
                        centralState = CentralVoiceState.LISTENING,
                        errorMessage = null,
                    )
                }
            }

            override fun onListeningStarted() {
                centralStateManager.notifyBeginningOfSpeech()
                _uiState.update { current ->
                    current.copy(
                        voiceState = VoiceState.LISTENING,
                        centralState = CentralVoiceState.LISTENING,
                    )
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                centralStateManager.notifyRmsChanged(rmsdB)
            }

            override fun onPartialResult(partialText: String) {
                if (partialText.isNotBlank()) {
                    centralStateManager.notifyPartialResult(partialText)
                    _uiState.update { current ->
                        current.copy(
                            interimSpeechText = partialText,
                            voiceState = VoiceState.LISTENING,
                            centralState = CentralVoiceState.LISTENING,
                        )
                    }
                }
            }

            override fun onFinalResult(finalText: String) {
                centralStateManager.notifySpeechFinalResult(finalText)
                _uiState.update { current ->
                    current.copy(
                        soundLevel = 0f,
                        interimSpeechText = "",
                    )
                }

                if (finalText.isBlank()) {
                    _uiState.update { current ->
                        current.copy(
                            voiceState = VoiceState.READY,
                            centralState = CentralVoiceState.IDLE,
                            errorMessage = "No speech detected. Tap orb or microphone to speak.",
                        )
                    }
                } else {
                    _uiState.update { current ->
                        current.copy(
                            recognizedText = finalText,
                            voiceState = VoiceState.RECOGNIZING,
                            centralState = CentralVoiceState.PROCESSING,
                        )
                    }
                    processAndSendPrompt(finalText)
                }
            }

            override fun onError(errorMessage: String, errorCode: Int) {
                val isTimeout = (errorCode == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                        errorCode == android.speech.SpeechRecognizer.ERROR_NO_MATCH)

                centralStateManager.notifyError(errorMessage, isTimeout = isTimeout)

                if (isTimeout) {
                    _uiState.update { current ->
                        current.copy(
                            voiceState = VoiceState.READY,
                            centralState = CentralVoiceState.IDLE,
                            soundLevel = 0f,
                            errorMessage = "No speech detected within time limit. Tap to try again.",
                        )
                    }
                } else {
                    _uiState.update { current ->
                        current.copy(
                            voiceState = VoiceState.ERROR,
                            centralState = CentralVoiceState.ERROR,
                            soundLevel = 0f,
                            errorMessage = errorMessage,
                        )
                    }
                }
            }
        })
    }

    private fun setupTtsCallbacks() {
        ttsManager.setCallback(object : TtsCallback {
            override fun onInitSuccess() {
                _uiState.update { current ->
                    current.copy(isTtsReady = true)
                }
            }

            override fun onInitError(message: String) {
                _uiState.update { current ->
                    current.copy(
                        isTtsReady = false,
                        errorMessage = message,
                    )
                }
            }

            override fun onSpeechStarted(utteranceId: String) {
                if (utteranceId == currentSessionId) {
                    // Do not report SPEAKING until text-to-speech has actually started audio playback
                    centralStateManager.notifyTtsStarted(utteranceId)
                    _uiState.update { current ->
                        current.copy(
                            voiceState = VoiceState.SPEAKING,
                            centralState = CentralVoiceState.SPEAKING,
                            isTtsSpeaking = true,
                        )
                    }
                }
            }

            override fun onSpeechFinished(utteranceId: String) {
                if (utteranceId == currentSessionId) {
                    centralStateManager.notifyTtsFinished(utteranceId)
                    _uiState.update { current ->
                        current.copy(
                            voiceState = VoiceState.READY,
                            centralState = CentralVoiceState.IDLE,
                            isTtsSpeaking = false,
                        )
                    }
                }
            }

            override fun onSpeechError(utteranceId: String, errorCode: Int) {
                if (utteranceId == currentSessionId) {
                    centralStateManager.notifyTtsError(utteranceId, errorCode)
                    _uiState.update { current ->
                        current.copy(
                            voiceState = VoiceState.READY,
                            centralState = CentralVoiceState.IDLE,
                            isTtsSpeaking = false,
                        )
                    }
                }
            }
        })
    }

    /**
     * Handles user tapping the central holographic orb or mic button.
     * Protected by debounce and authoritative transition validation.
     */
    fun onMicTapped() {
        if (!centralStateManager.canAcceptTap()) {
            return
        }

        when (centralStateManager.currentState) {
            CentralVoiceState.LISTENING -> {
                speechManager.stopListening()
                centralStateManager.notifyEndOfSpeech()
                _uiState.update { current ->
                    current.copy(
                        voiceState = VoiceState.RECOGNIZING,
                        centralState = CentralVoiceState.PROCESSING,
                    )
                }
            }
            CentralVoiceState.SPEAKING,
            CentralVoiceState.PROCESSING,
            CentralVoiceState.EXECUTING_TASK -> {
                stopActiveSession()
            }
            CentralVoiceState.IDLE, CentralVoiceState.ERROR -> {
                if (!_uiState.value.isPermissionGranted) {
                    _uiState.update { current ->
                        current.copy(
                            voiceState = VoiceState.REQUESTING_PERMISSION,
                            centralState = CentralVoiceState.STARTING,
                        )
                    }
                } else {
                    startListening()
                }
            }
            CentralVoiceState.STARTING, CentralVoiceState.STOPPING -> {
                // Ignore transient initialization taps
            }
        }
    }

    fun startListening() {
        if (!_uiState.value.isSpeechAvailable) {
            centralStateManager.notifyError("Speech recognition is not supported on this device.")
            _uiState.update { current ->
                current.copy(
                    voiceState = VoiceState.ERROR,
                    centralState = CentralVoiceState.ERROR,
                    errorMessage = "Speech recognition is not supported on this device.",
                )
            }
            return
        }

        val accepted = centralStateManager.requestStartListening(hasPermission = true)
        if (!accepted) return

        currentSessionId = UUID.randomUUID().toString()
        stopTtsAndAi()

        _uiState.update { current ->
            current.copy(
                voiceState = VoiceState.REQUESTING_PERMISSION,
                centralState = CentralVoiceState.STARTING,
                recognizedText = "",
                interimSpeechText = "",
                errorMessage = null,
                soundLevel = 0f,
            )
        }

        speechManager.startListening(_uiState.value.activeLanguage)
    }

    fun stopActiveSession() {
        currentSessionId = UUID.randomUUID().toString()
        speechManager.cancel()
        stopTtsAndAi()
        centralStateManager.stopAll()
        _uiState.update { current ->
            current.copy(
                voiceState = VoiceState.READY,
                centralState = CentralVoiceState.IDLE,
                soundLevel = 0f,
                isTtsSpeaking = false,
            )
        }
    }

    private fun stopTtsAndAi() {
        activeAiJob?.cancel()
        activeAiJob = null
        ttsManager.stop()
    }

    /**
     * Toggles incognito mode for voice conversations.
     * When active, turns and memories are not written to persistent storage.
     */
    fun toggleIncognito(enabled: Boolean) {
        _uiState.update { it.copy(isIncognito = enabled) }
    }

    fun processAndSendPrompt(prompt: String) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) return

        val thisSessionId = UUID.randomUUID().toString()
        currentSessionId = thisSessionId

        val convId = _uiState.value.conversationId
        val userMessage = ChatMessage(
            conversationId = convId,
            role = ChatRole.User,
            content = cleanPrompt,
            status = MessageStatus.Sent,
        )

        centralStateManager.notifyProcessing()
        _uiState.update { current ->
            current.copy(
                voiceState = VoiceState.THINKING,
                centralState = CentralVoiceState.PROCESSING,
                recognizedText = cleanPrompt,
                errorMessage = null,
            )
        }

        val isIncognito = _uiState.value.isIncognito

        if (!isIncognito) {
            viewModelScope.launch {
                conversationRepository.saveUserTurn(convId, userMessage)
            }
        }

        activeAiJob?.cancel()
        activeAiJob = viewModelScope.launch {
            // Check AI Tool Registry functions (Phase 15: Sections A-I)
            val matchingTool = aiToolRegistry.findMatchingTool(cleanPrompt)
            if (matchingTool != null) {
                centralStateManager.notifyExecutingTask(true)
                _uiState.update { current ->
                    current.copy(
                        voiceState = VoiceState.THINKING,
                        centralState = CentralVoiceState.EXECUTING_TASK,
                    )
                }

                val toolResult = aiToolRegistry.executeTool(matchingTool)
                val replyText = toolResult.message

                if (currentSessionId != thisSessionId) return@launch

                val assistantMsg = ChatMessage(
                    conversationId = convId,
                    role = ChatRole.Assistant,
                    content = replyText,
                    status = MessageStatus.Complete,
                )
                if (!isIncognito) {
                    conversationRepository.saveAssistantTurn(convId, assistantMsg)
                }

                val prefs = preferencesRepository.getPreferences()
                if (prefs.ttsEnabled) {
                    centralStateManager.notifyProcessing()
                    _uiState.update { current ->
                        current.copy(
                            assistantResponseText = replyText,
                            errorMessage = null,
                        )
                    }
                    ttsManager.speak(
                        text = replyText,
                        utteranceId = thisSessionId,
                        language = _uiState.value.activeLanguage,
                        rate = prefs.speechRate,
                        pitch = prefs.speechPitch,
                        voiceId = prefs.ttsVoiceId,
                    )
                } else {
                    centralStateManager.notifyExecutingTask(false)
                    _uiState.update { current ->
                        current.copy(
                            voiceState = VoiceState.READY,
                            centralState = CentralVoiceState.IDLE,
                            isTtsSpeaking = false,
                            assistantResponseText = replyText,
                            errorMessage = null,
                        )
                    }
                }
                return@launch
            }

            // Check Accessibility Automation command routing (Phase 11)
            if (automationCoordinator.isAutomationCommand(cleanPrompt)) {
                centralStateManager.notifyExecutingTask(true)
                _uiState.update { current ->
                    current.copy(
                        voiceState = VoiceState.THINKING,
                        centralState = CentralVoiceState.EXECUTING_TASK,
                    )
                }

                automationCoordinator.submitCommand(cleanPrompt)
                val report = automationCoordinator.uiState.value.lastExecutionReport
                val replyText = report?.summary
                    ?: if (automationCoordinator.uiState.value.taskStatus == com.evai.rifatcoder.domain.automation.AutomationTaskStatus.WaitingForConfirmation) {
                        "This command requires confirmation: ${automationCoordinator.uiState.value.pendingConfirmationReason}. Please confirm on the screen."
                    } else {
                        "Executing automation task for \"$cleanPrompt\"..."
                    }

                if (currentSessionId != thisSessionId) return@launch

                val assistantMsg = ChatMessage(
                    conversationId = convId,
                    role = ChatRole.Assistant,
                    content = replyText,
                    status = MessageStatus.Complete,
                )
                conversationRepository.saveAssistantTurn(convId, assistantMsg)

                val prefs = preferencesRepository.getPreferences()
                if (prefs.ttsEnabled) {
                    centralStateManager.notifyProcessing()
                    _uiState.update { current ->
                        current.copy(
                            assistantResponseText = replyText,
                            errorMessage = null,
                        )
                    }
                    ttsManager.speak(
                        text = replyText,
                        utteranceId = thisSessionId,
                        language = _uiState.value.activeLanguage,
                        rate = prefs.speechRate,
                        pitch = prefs.speechPitch,
                        voiceId = prefs.ttsVoiceId,
                    )
                } else {
                    centralStateManager.notifyExecutingTask(false)
                    _uiState.update { current ->
                        current.copy(
                            voiceState = VoiceState.READY,
                            centralState = CentralVoiceState.IDLE,
                            isTtsSpeaking = false,
                            assistantResponseText = replyText,
                            errorMessage = null,
                        )
                    }
                }
                return@launch
            }

            // Standard AI Chat Engine flow with unified 3-layer context
            val relevantMemories = if (!isIncognito && memoryRepository.isMemoryEnabled()) {
                memoryRepository.getRelevantMemoriesForContext(cleanPrompt)
            } else {
                emptyList()
            }

            val preparedContext = contextBuilder.buildContext(
                conversationHistory = listOf(userMessage),
                userPrompt = cleanPrompt,
                relevantMemories = relevantMemories,
                isIncognito = isIncognito,
                userPreferredLanguage = _uiState.value.activeLanguage.displayName,
            )

            val result = chatEngine.sendPrompt(
                conversationId = convId,
                history = preparedContext.boundedHistory,
                prompt = preparedContext.finalPrompt,
                memoryContext = null,
            )

            if (currentSessionId != thisSessionId) return@launch

            result.fold(
                onSuccess = { replyText ->
                    val assistantMsg = ChatMessage(
                        conversationId = convId,
                        role = ChatRole.Assistant,
                        content = replyText,
                        status = MessageStatus.Complete,
                    )

                    if (!isIncognito) {
                        conversationRepository.saveAssistantTurn(convId, assistantMsg)
                    }
                    centralStateManager.setAssistantResponse(replyText)

                    val prefs = preferencesRepository.getPreferences()
                    if (prefs.ttsEnabled) {
                        // Stay in PROCESSING until TTS engine fires onSpeechStarted
                        _uiState.update { current ->
                            current.copy(
                                assistantResponseText = replyText,
                                errorMessage = null,
                            )
                        }
                        ttsManager.speak(
                            text = replyText,
                            utteranceId = thisSessionId,
                            language = _uiState.value.activeLanguage,
                            rate = prefs.speechRate,
                            pitch = prefs.speechPitch,
                            voiceId = prefs.ttsVoiceId,
                        )
                    } else {
                        centralStateManager.resetToIdle()
                        _uiState.update { current ->
                            current.copy(
                                voiceState = VoiceState.READY,
                                centralState = CentralVoiceState.IDLE,
                                isTtsSpeaking = false,
                                assistantResponseText = replyText,
                                errorMessage = null,
                            )
                        }
                    }
                },
                onFailure = { error ->
                    val noticeMsg = ChatMessage(
                        conversationId = convId,
                        role = ChatRole.Assistant,
                        content = error.message ?: "Failed to generate AI response.",
                        status = MessageStatus.Failed,
                    )
                    conversationRepository.saveAssistantTurn(convId, noticeMsg)

                    val friendlyError = error.localizedMessage ?: "AI connection failed. Please check network or API settings."
                    centralStateManager.notifyError(friendlyError)

                    _uiState.update { current ->
                        current.copy(
                            voiceState = VoiceState.ERROR,
                            centralState = CentralVoiceState.ERROR,
                            errorMessage = friendlyError,
                        )
                    }
                }
            )
        }
    }

    fun toggleLanguage() {
        val currentLang = _uiState.value.activeLanguage
        val newLang = when (currentLang) {
            VoiceLanguage.ENGLISH -> VoiceLanguage.BENGALI
            VoiceLanguage.BENGALI -> VoiceLanguage.ENGLISH
        }
        _uiState.update { current ->
            current.copy(activeLanguage = newLang)
        }
    }

    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { current ->
            current.copy(isPermissionGranted = granted)
        }
        if (granted) {
            startListening()
        } else {
            centralStateManager.notifyError("Microphone permission is required for voice interaction.")
            _uiState.update { current ->
                current.copy(
                    voiceState = VoiceState.ERROR,
                    centralState = CentralVoiceState.ERROR,
                    errorMessage = "Microphone permission is required for voice interaction.",
                )
            }
        }
    }

    fun replayResponse() {
        val text = _uiState.value.assistantResponseText
        if (text.isNotBlank()) {
            val prefs = preferencesRepository.getPreferences()
            if (!prefs.ttsEnabled) return

            currentSessionId = UUID.randomUUID().toString()
            ttsManager.speak(
                text = text,
                utteranceId = currentSessionId,
                language = _uiState.value.activeLanguage,
                rate = prefs.speechRate,
                pitch = prefs.speechPitch,
                voiceId = prefs.ttsVoiceId,
            )
        }
    }

    fun updateTranscriptionText(text: String) {
        _uiState.update { current ->
            current.copy(recognizedText = text)
        }
    }

    fun submitManualPrompt() {
        val text = _uiState.value.recognizedText
        if (text.isNotBlank()) {
            processAndSendPrompt(text)
        }
    }

    fun dismissError() {
        centralStateManager.resetToIdle()
        _uiState.update { current ->
            current.copy(
                errorMessage = null,
                voiceState = if (current.voiceState == VoiceState.ERROR) VoiceState.READY else current.voiceState,
                centralState = if (current.centralState == CentralVoiceState.ERROR) CentralVoiceState.IDLE else current.centralState,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        ttsManager.shutdown()
        centralStateManager.resetToIdle()
    }
}
