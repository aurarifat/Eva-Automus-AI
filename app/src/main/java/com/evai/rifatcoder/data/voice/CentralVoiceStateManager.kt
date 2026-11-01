package com.evai.rifatcoder.data.voice

import com.evai.rifatcoder.data.glow.EdgeGlowStateHolder
import com.evai.rifatcoder.domain.glow.EdgeGlowState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Authoritative Central Voice States for EVA AI (Phase 13).
 * Represents the single source of truth across UI, Background, STT, TTS,
 * Holographic Particle Orb, and Four-Side Edge Glow Overlay.
 */
enum class CentralVoiceState {
    IDLE,
    STARTING,
    LISTENING,
    PROCESSING,
    SPEAKING,
    EXECUTING_TASK,
    STOPPING,
    ERROR;

    val displayName: String
        get() = when (this) {
            IDLE -> "Idle"
            STARTING -> "Starting..."
            LISTENING -> "Listening"
            PROCESSING -> "Thinking"
            SPEAKING -> "Speaking"
            EXECUTING_TASK -> "Executing Task"
            STOPPING -> "Stopping..."
            ERROR -> "Error"
        }

    fun toEdgeGlowState(): EdgeGlowState = when (this) {
        IDLE, STOPPING -> EdgeGlowState.IDLE
        STARTING -> EdgeGlowState.IDLE
        LISTENING -> EdgeGlowState.LISTENING
        PROCESSING -> EdgeGlowState.THINKING
        SPEAKING -> EdgeGlowState.SPEAKING
        EXECUTING_TASK -> EdgeGlowState.EXECUTING
        ERROR -> EdgeGlowState.ERROR
    }

    fun toLegacyVoiceState(): VoiceState = when (this) {
        IDLE, STOPPING -> VoiceState.READY
        STARTING -> VoiceState.REQUESTING_PERMISSION
        LISTENING -> VoiceState.LISTENING
        PROCESSING -> VoiceState.THINKING
        SPEAKING -> VoiceState.SPEAKING
        EXECUTING_TASK -> VoiceState.THINKING
        ERROR -> VoiceState.ERROR
    }
}

/**
 * Central, thread-safe, lifecycle-aware Voice State Manager for EVA AI.
 *
 * Guarantees:
 * 1. Single authoritative state machine for voice and visual feedback.
 * 2. Prevents contradictory states (e.g. LISTENING while SPEAKING).
 * 3. Prevents duplicate microphone sessions.
 * 4. Debounces rapid repeated mic taps safely.
 * 5. Does not report LISTENING until speech recognition is genuinely active (onReady).
 * 6. Does not report SPEAKING until TTS has actually started (onSpeechStarted).
 * 7. Resets cleanly to IDLE when speech ends, TTS finishes, or user cancels.
 * 8. Synchronizes Holographic Particle Orb and Edge Glow automatically.
 */
object CentralVoiceStateManager {

    private val _state = MutableStateFlow(CentralVoiceState.IDLE)
    val state: StateFlow<CentralVoiceState> = _state.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>("Tap orb or microphone to speak")
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()

    private val _interimSpeech = MutableStateFlow("")
    val interimSpeech: StateFlow<String> = _interimSpeech.asStateFlow()

    private val _assistantResponse = MutableStateFlow("")
    val assistantResponse: StateFlow<String> = _assistantResponse.asStateFlow()

    // Rapid-tap debounce tracking (350ms window)
    private var lastTapTimestamp: Long = 0L
    private const val DEBOUNCE_INTERVAL_MS = 350L

    val currentState: CentralVoiceState
        get() = _state.value

    val currentSoundLevel: Float
        get() = _soundLevel.value

    /**
     * Checks if a user tap on the microphone/orb should be accepted.
     * Rejects rapid multi-taps within the debounce interval.
     */
    @Synchronized
    fun canAcceptTap(currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        if (currentTimeMs - lastTapTimestamp < DEBOUNCE_INTERVAL_MS) {
            return false
        }
        lastTapTimestamp = currentTimeMs
        return true
    }

    /**
     * Requests initiation of a voice session.
     * Transitions from IDLE or ERROR to STARTING.
     * Rejects requests if already LISTENING or STARTING (prevents duplicate sessions).
     */
    @Synchronized
    fun requestStartListening(hasPermission: Boolean): Boolean {
        val current = _state.value
        if (current == CentralVoiceState.STARTING || current == CentralVoiceState.LISTENING) {
            // Already starting or listening — ignore duplicate session request
            return false
        }

        if (!hasPermission) {
            _state.value = CentralVoiceState.STARTING
            _statusMessage.value = "Microphone permission required"
            return true
        }

        _state.value = CentralVoiceState.STARTING
        _soundLevel.value = 0f
        _errorMessage.value = null
        _interimSpeech.value = ""
        _transcribedText.value = ""
        _statusMessage.value = "Preparing voice engine..."
        syncEdgeGlow()
        return true
    }

    /**
     * Called when the speech recognizer is genuinely ready to receive audio.
     * Transitions state from STARTING to LISTENING.
     */
    @Synchronized
    fun notifyReadyForSpeech() {
        if (_state.value == CentralVoiceState.STARTING || _state.value == CentralVoiceState.IDLE) {
            _state.value = CentralVoiceState.LISTENING
            _statusMessage.value = "Listening to your voice..."
            syncEdgeGlow()
        }
    }

    /**
     * Directly transitions to LISTENING state (e.g. from Dashboard Voice Mode toggle).
     */
    @Synchronized
    fun notifyListening() {
        _state.value = CentralVoiceState.LISTENING
        _statusMessage.value = "Listening to your voice..."
        syncEdgeGlow()
    }

    /**
     * Called when speech beginning is detected.
     */
    @Synchronized
    fun notifyBeginningOfSpeech() {
        _state.value = CentralVoiceState.LISTENING
        _statusMessage.value = "Listening..."
        syncEdgeGlow()
    }

    /**
     * Updates RMS sound level (0.0 to 1.0) while actively listening.
     */
    @Synchronized
    fun notifyRmsChanged(rmsdB: Float) {
        if (_state.value == CentralVoiceState.LISTENING) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _soundLevel.value = normalized
            EdgeGlowStateHolder.updateVoiceState(_state.value, normalized)
        }
    }

    /**
     * Updates interim partial speech recognition results.
     */
    @Synchronized
    fun notifyPartialResult(partialText: String) {
        if (partialText.isNotBlank()) {
            _interimSpeech.value = partialText
            if (_state.value == CentralVoiceState.STARTING) {
                _state.value = CentralVoiceState.LISTENING
            }
        }
    }

    /**
     * Called when speech input finishes (end of speech detected).
     * Transitions from LISTENING to PROCESSING.
     */
    @Synchronized
    fun notifyEndOfSpeech() {
        if (_state.value == CentralVoiceState.LISTENING) {
            _state.value = CentralVoiceState.PROCESSING
            _soundLevel.value = 0f
            _statusMessage.value = "Processing speech..."
            syncEdgeGlow()
        }
    }

    /**
     * Called when final speech transcript is received.
     * If blank: resets cleanly to IDLE.
     * If text present: sets PROCESSING and stores transcribed text.
     */
    @Synchronized
    fun notifySpeechFinalResult(finalText: String) {
        _soundLevel.value = 0f
        _interimSpeech.value = ""
        val clean = finalText.trim()

        if (clean.isBlank()) {
            _state.value = CentralVoiceState.IDLE
            _statusMessage.value = "No speech detected. Tap orb or microphone to speak."
            _errorMessage.value = null
            syncEdgeGlow()
        } else {
            _transcribedText.value = clean
            _state.value = CentralVoiceState.PROCESSING
            _statusMessage.value = "Thinking..."
            syncEdgeGlow()
        }
    }

    /**
     * Notifies that an AI request or task is being processed.
     */
    @Synchronized
    fun notifyProcessing() {
        _state.value = CentralVoiceState.PROCESSING
        _soundLevel.value = 0f
        _statusMessage.value = "Thinking..."
        syncEdgeGlow()
    }

    /**
     * Notifies that an accessibility automation task is actively executing.
     */
    @Synchronized
    fun notifyExecutingTask(isExecuting: Boolean) {
        if (isExecuting) {
            _state.value = CentralVoiceState.EXECUTING_TASK
            _soundLevel.value = 0.5f
            _statusMessage.value = "Executing automation task..."
            EdgeGlowStateHolder.setAutomationExecuting(true)
        } else {
            EdgeGlowStateHolder.setAutomationExecuting(false)
            _soundLevel.value = 0f
            _state.value = CentralVoiceState.IDLE
            _statusMessage.value = "Task completed"
            syncEdgeGlow()
        }
    }

    /**
     * Called when text-to-speech audio playback genuinely begins.
     * Transitions state from PROCESSING to SPEAKING.
     */
    @Synchronized
    fun notifyTtsStarted(utteranceId: String) {
        _state.value = CentralVoiceState.SPEAKING
        _soundLevel.value = 0.35f
        _statusMessage.value = "EVA is speaking..."
        syncEdgeGlow()
    }

    /**
     * Called when TTS audio finishes playing.
     * Resets cleanly to IDLE.
     */
    @Synchronized
    fun notifyTtsFinished(utteranceId: String) {
        if (_state.value == CentralVoiceState.SPEAKING) {
            _state.value = CentralVoiceState.IDLE
            _soundLevel.value = 0f
            _statusMessage.value = "Tap orb or microphone to speak"
            syncEdgeGlow()
        }
    }

    /**
     * Called when TTS audio fails or is interrupted.
     * Resets cleanly to IDLE without leaving the app stuck in SPEAKING.
     */
    @Synchronized
    fun notifyTtsError(utteranceId: String, errorCode: Int) {
        _soundLevel.value = 0f
        _state.value = CentralVoiceState.IDLE
        _statusMessage.value = "Tap orb or microphone to speak"
        syncEdgeGlow()
    }

    /**
     * Handles speech recognition errors, network failures, or permission denials.
     * If timeout or no speech detected, resets gently to IDLE.
     */
    @Synchronized
    fun notifyError(message: String, isTimeout: Boolean = false) {
        _soundLevel.value = 0f
        if (isTimeout) {
            _state.value = CentralVoiceState.IDLE
            _statusMessage.value = "No speech detected within time limit. Tap to try again."
            _errorMessage.value = null
        } else {
            _state.value = CentralVoiceState.ERROR
            _errorMessage.value = message
            _statusMessage.value = message
        }
        syncEdgeGlow()
    }

    /**
     * Stops all active voice processing, recognition, and TTS.
     * Cleanly transitions through STOPPING to IDLE.
     */
    @Synchronized
    fun stopAll() {
        _state.value = CentralVoiceState.STOPPING
        _soundLevel.value = 0f
        _interimSpeech.value = ""
        _state.value = CentralVoiceState.IDLE
        _statusMessage.value = "Tap orb or microphone to speak"
        _errorMessage.value = null
        EdgeGlowStateHolder.setAutomationExecuting(false)
        syncEdgeGlow()
    }

    /**
     * Sets the assistant response text for display.
     */
    @Synchronized
    fun setAssistantResponse(response: String) {
        _assistantResponse.value = response
    }

    /**
     * Resets the manager state to clean initial defaults.
     */
    @Synchronized
    fun resetToIdle() {
        _state.value = CentralVoiceState.IDLE
        _soundLevel.value = 0f
        _errorMessage.value = null
        _interimSpeech.value = ""
        _statusMessage.value = "Tap orb or microphone to speak"
        EdgeGlowStateHolder.setAutomationExecuting(false)
        syncEdgeGlow()
    }

    /**
     * Synchronizes EdgeGlowStateHolder with the current authoritative state.
     */
    private fun syncEdgeGlow() {
        val current = _state.value
        val sound = _soundLevel.value
        if (current == CentralVoiceState.EXECUTING_TASK) {
            EdgeGlowStateHolder.setAutomationExecuting(true)
        } else {
            EdgeGlowStateHolder.updateVoiceState(current, sound)
        }
    }
}
