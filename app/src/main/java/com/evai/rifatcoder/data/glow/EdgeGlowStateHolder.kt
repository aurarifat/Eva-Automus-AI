package com.evai.rifatcoder.data.glow

import com.evai.rifatcoder.data.voice.CentralVoiceState
import com.evai.rifatcoder.data.voice.VoiceState
import com.evai.rifatcoder.domain.automation.AutomationTaskStatus
import com.evai.rifatcoder.domain.glow.EdgeGlowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Authoritative Central State Synchronization Engine for Edge Glow (Phase 15).
 * Coordinates real-time Voice States, AI Tool Registry, and Task Execution Manager.
 *
 * Requirements enforced:
 * 1. Single reliable source of truth across all components.
 * 2. Strict state arbitration without conflicting controllers.
 * 3. Genuine task progress updates with step tracking.
 * 4. Distinct animations for WAITING_FOR_CONFIRMATION, EXECUTING, SUCCESS, FAILED, and CANCELLED.
 * 5. Persistent dashboard voice mode management.
 */
object EdgeGlowStateHolder {

    private val holderScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _glowState = MutableStateFlow(EdgeGlowState.IDLE)
    val glowState: StateFlow<EdgeGlowState> = _glowState.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    // Real task execution telemetry
    private val _currentTaskStatus = MutableStateFlow(AutomationTaskStatus.Idle)
    val currentTaskStatus: StateFlow<AutomationTaskStatus> = _currentTaskStatus.asStateFlow()

    private val _taskStepProgress = MutableStateFlow(0f)
    val taskStepProgress: StateFlow<Float> = _taskStepProgress.asStateFlow()

    private val _taskStatusMessage = MutableStateFlow<String?>(null)
    val taskStatusMessage: StateFlow<String?> = _taskStatusMessage.asStateFlow()

    private val _isPersistentVoiceModeActive = MutableStateFlow(false)
    val isPersistentVoiceModeActive: StateFlow<Boolean> = _isPersistentVoiceModeActive.asStateFlow()

    private var currentVoiceState: CentralVoiceState = CentralVoiceState.IDLE
    private var isSimulating: Boolean = false
    private var transientFeedbackJob: Job? = null

    /**
     * Toggles the dashboard persistent voice mode.
     * When ON, the four-side edge glow shows active listening animation and screen reading capability.
     * Stays active until user turns it OFF from dashboard.
     */
    fun setPersistentVoiceMode(enabled: Boolean) {
        _isPersistentVoiceModeActive.value = enabled
        recalculateEffectiveState()
    }

    /**
     * Updates real-time Central Voice Engine state.
     */
    fun updateVoiceState(centralState: CentralVoiceState, currentSoundLevel: Float = 0f) {
        if (isSimulating) return
        val clampedSound = currentSoundLevel.coerceIn(0f, 1f)
        _soundLevel.value = clampedSound
        currentVoiceState = centralState
        recalculateEffectiveState()
    }

    /**
     * Legacy compatibility bridge for VoiceState.
     */
    fun updateVoiceState(voiceState: VoiceState, currentSoundLevel: Float = 0f) {
        if (isSimulating) return
        val clampedSound = currentSoundLevel.coerceIn(0f, 1f)
        _soundLevel.value = clampedSound

        currentVoiceState = when (voiceState) {
            VoiceState.READY, VoiceState.REQUESTING_PERMISSION -> CentralVoiceState.IDLE
            VoiceState.LISTENING -> CentralVoiceState.LISTENING
            VoiceState.RECOGNIZING, VoiceState.THINKING -> CentralVoiceState.PROCESSING
            VoiceState.SPEAKING -> CentralVoiceState.SPEAKING
            VoiceState.ERROR -> CentralVoiceState.ERROR
        }
        recalculateEffectiveState()
    }

    // ==========================================
    // TASK EXECUTION MANAGER SYNCHRONIZATION
    // ==========================================

    fun onTaskPlanning(prompt: String) {
        cancelTransientFeedback()
        _currentTaskStatus.value = AutomationTaskStatus.Planning
        _taskStatusMessage.value = "Planning: $prompt"
        _taskStepProgress.value = 0f
        recalculateEffectiveState()
    }

    fun onTaskWaitingForConfirmation(prompt: String, reason: String) {
        cancelTransientFeedback()
        _currentTaskStatus.value = AutomationTaskStatus.WaitingForConfirmation
        _taskStatusMessage.value = "Confirmation required: $reason"
        recalculateEffectiveState()
    }

    fun onTaskExecuting(stepIndex: Int, totalSteps: Int, description: String) {
        cancelTransientFeedback()
        _currentTaskStatus.value = AutomationTaskStatus.Running
        _taskStatusMessage.value = "Step $stepIndex of $totalSteps: $description"
        _taskStepProgress.value = if (totalSteps > 0) stepIndex.toFloat() / totalSteps.toFloat() else 0.5f
        recalculateEffectiveState()
    }

    fun onTaskStepCompleted(stepIndex: Int, totalSteps: Int) {
        _taskStepProgress.value = if (totalSteps > 0) stepIndex.toFloat() / totalSteps.toFloat() else 1f
    }

    fun onTaskSuccess(summary: String, returnDelayMs: Long = 1500L) {
        cancelTransientFeedback()
        _currentTaskStatus.value = AutomationTaskStatus.Completed
        _taskStatusMessage.value = summary
        _taskStepProgress.value = 1.0f

        _glowState.value = EdgeGlowState.SUCCESS
        _soundLevel.value = 0.5f

        transientFeedbackJob = holderScope.launch {
            delay(returnDelayMs)
            _currentTaskStatus.value = AutomationTaskStatus.Idle
            _taskStatusMessage.value = null
            _taskStepProgress.value = 0f
            transientFeedbackJob = null
            recalculateEffectiveState()
        }
    }

    fun onTaskFailed(error: String, returnDelayMs: Long = 1800L) {
        cancelTransientFeedback()
        _currentTaskStatus.value = AutomationTaskStatus.Failed
        _taskStatusMessage.value = error

        _glowState.value = EdgeGlowState.FAILED
        _soundLevel.value = 0.3f

        transientFeedbackJob = holderScope.launch {
            delay(returnDelayMs)
            _currentTaskStatus.value = AutomationTaskStatus.Idle
            _taskStatusMessage.value = null
            _taskStepProgress.value = 0f
            transientFeedbackJob = null
            recalculateEffectiveState()
        }
    }

    fun onTaskCancelled(returnDelayMs: Long = 1200L) {
        cancelTransientFeedback()
        _currentTaskStatus.value = AutomationTaskStatus.Cancelled
        _taskStatusMessage.value = "Task cancelled by user"

        _glowState.value = EdgeGlowState.CANCELLED
        _soundLevel.value = 0f

        transientFeedbackJob = holderScope.launch {
            delay(returnDelayMs)
            _currentTaskStatus.value = AutomationTaskStatus.Idle
            _taskStatusMessage.value = null
            _taskStepProgress.value = 0f
            transientFeedbackJob = null
            recalculateEffectiveState()
        }
    }

    /**
     * Backward-compatible helper for basic automation execution toggle.
     */
    fun setAutomationExecuting(isExecuting: Boolean) {
        if (isExecuting) {
            onTaskExecuting(1, 1, "Executing task...")
        } else if (_currentTaskStatus.value == AutomationTaskStatus.Running) {
            _currentTaskStatus.value = AutomationTaskStatus.Idle
            recalculateEffectiveState()
        }
    }

    fun triggerErrorAlert() {
        cancelTransientFeedback()
        _currentTaskStatus.value = AutomationTaskStatus.Failed
        _taskStatusMessage.value = "Action failed or permission missing"

        _glowState.value = EdgeGlowState.ERROR
        _soundLevel.value = 0.3f

        transientFeedbackJob = holderScope.launch {
            delay(1500L)
            _currentTaskStatus.value = AutomationTaskStatus.Idle
            _taskStatusMessage.value = null
            _taskStepProgress.value = 0f
            transientFeedbackJob = null
            recalculateEffectiveState()
        }
    }

    fun resetToIdle() {
        cancelTransientFeedback()
        isSimulating = false
        _isPersistentVoiceModeActive.value = false
        _currentTaskStatus.value = AutomationTaskStatus.Idle
        _taskStatusMessage.value = null
        _taskStepProgress.value = 0f
        currentVoiceState = CentralVoiceState.IDLE
        _glowState.value = EdgeGlowState.IDLE
        _soundLevel.value = 0f
    }

    // ==========================================
    // SIMULATION (SETTINGS PREVIEWS)
    // ==========================================

    fun simulateState(state: EdgeGlowState, simulatedSoundLevel: Float = 0.6f) {
        cancelTransientFeedback()
        isSimulating = true
        _glowState.value = state
        _soundLevel.value = when (state) {
            EdgeGlowState.SPEAKING, EdgeGlowState.LISTENING -> simulatedSoundLevel.coerceIn(0f, 1f)
            EdgeGlowState.EXECUTING -> 0.4f
            EdgeGlowState.WAITING_FOR_CONFIRMATION -> 0.35f
            EdgeGlowState.SUCCESS -> 0.5f
            EdgeGlowState.FAILED, EdgeGlowState.ERROR -> 0.3f
            EdgeGlowState.CANCELLED -> 0.1f
            else -> 0f
        }
    }

    fun stopSimulation() {
        isSimulating = false
        recalculateEffectiveState()
    }

    // ==========================================
    // STATE ARBITRATION ENGINE
    // ==========================================

    private fun recalculateEffectiveState() {
        if (isSimulating) return
        if (transientFeedbackJob?.isActive == true) return

        val taskStatus = _currentTaskStatus.value

        val effectiveState = when {
            // 1. Task Execution & Verification has highest priority
            taskStatus == AutomationTaskStatus.Running || taskStatus == AutomationTaskStatus.Verifying -> {
                EdgeGlowState.EXECUTING
            }
            // 2. User confirmation waiting
            taskStatus == AutomationTaskStatus.WaitingForConfirmation -> {
                EdgeGlowState.WAITING_FOR_CONFIRMATION
            }
            // 3. Task Planning
            taskStatus == AutomationTaskStatus.Planning -> {
                EdgeGlowState.THINKING
            }
            // 4. Voice Speaking
            currentVoiceState == CentralVoiceState.SPEAKING -> {
                EdgeGlowState.SPEAKING
            }
            // 5. Voice Listening
            currentVoiceState == CentralVoiceState.LISTENING -> {
                EdgeGlowState.LISTENING
            }
            // 6. Voice Processing
            currentVoiceState == CentralVoiceState.PROCESSING -> {
                EdgeGlowState.THINKING
            }
            // 7. Voice Starting / Executing task
            currentVoiceState == CentralVoiceState.EXECUTING_TASK -> {
                EdgeGlowState.EXECUTING
            }
            // 8. Voice Error
            currentVoiceState == CentralVoiceState.ERROR -> {
                EdgeGlowState.ERROR
            }
            // 9. Persistent Dashboard Voice Mode (stays listening with edge glow active)
            _isPersistentVoiceModeActive.value -> {
                EdgeGlowState.LISTENING
            }
            // 10. Default Idle
            else -> {
                EdgeGlowState.IDLE
            }
        }

        _glowState.value = effectiveState
    }

    private fun cancelTransientFeedback() {
        transientFeedbackJob?.cancel()
        transientFeedbackJob = null
    }
}
