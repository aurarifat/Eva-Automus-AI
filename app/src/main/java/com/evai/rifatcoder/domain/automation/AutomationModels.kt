package com.evai.rifatcoder.domain.automation

import android.graphics.Rect

/**
 * Enumeration of supported automation task states.
 */
enum class AutomationTaskStatus {
    Idle,
    Planning,
    WaitingForConfirmation,
    Running,
    Verifying,
    Completed,
    Failed,
    Cancelled,
}

/**
 * Level of consequential risk associated with an automation action.
 */
enum class SafetyRiskLevel {
    Safe,             // Standard read, navigation, volume, local app launch
    NeedsInspection,  // Requires verifying target node first
    Consequential,    // Sends message, purchases, deletes data, modifies security
}

/**
 * Direction for scrolling actions.
 */
enum class ScrollDirection {
    FORWARD,
    BACKWARD,
}

/**
 * Direction for audio volume adjustment.
 */
enum class VolumeDirection {
    UP,
    DOWN,
    MUTE,
    UNMUTE,
}

/**
 * Category of device status information.
 */
enum class DeviceStatusType {
    BATTERY,
    NETWORK,
    STORAGE,
    DATE_TIME,
}

/**
 * Simplified representation of an accessible node extracted from active window.
 */
data class AccessibleNode(
    val id: String,
    val text: String?,
    val contentDescription: String?,
    val className: String,
    val bounds: Rect,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val isEditable: Boolean,
    val isPassword: Boolean,
    val isEnabled: Boolean = true,
    val isFocused: Boolean = false,
    val viewIdResourceName: String? = null,
) {
    val displayLabel: String
        get() = text?.trim()?.ifEmpty { null }
            ?: contentDescription?.trim()?.ifEmpty { null }
            ?: viewIdResourceName?.substringAfterLast('/')
            ?: className.substringAfterLast('.')
}

/**
 * Supported fixed allowlist of atomic automation actions.
 */
sealed class AutomationAction {
    data class OpenApp(
        val appQuery: String,
        val packageName: String? = null,
    ) : AutomationAction()

    data object InspectScreen : AutomationAction()

    data class ClickNode(
        val targetText: String? = null,
        val targetId: String? = null,
        val targetDescription: String? = null,
    ) : AutomationAction()

    data class HoldNode(
        val targetText: String? = null,
        val targetId: String? = null,
        val targetDescription: String? = null,
    ) : AutomationAction()

    data class InputText(
        val targetText: String? = null,
        val targetId: String? = null,
        val textToInput: String = "",
    ) : AutomationAction()

    data class Scroll(
        val direction: ScrollDirection = ScrollDirection.FORWARD,
    ) : AutomationAction()

    data object NavigateBack : AutomationAction()

    data object NavigateHome : AutomationAction()

    data class AdjustVolume(
        val direction: VolumeDirection,
        val streamType: Int = 3, // AudioManager.STREAM_MUSIC
    ) : AutomationAction()

    data class SetAlarm(
        val hour: Int,
        val minute: Int,
        val message: String = "EVA AI Alarm",
    ) : AutomationAction()

    data class ToggleFlashlight(
        val enabled: Boolean,
    ) : AutomationAction()

    data class CheckDeviceStatus(
        val statusType: DeviceStatusType,
    ) : AutomationAction()

    data class WaitTransition(
        val durationMs: Long = 1000L,
    ) : AutomationAction()

    data class AskConfirmation(
        val prompt: String,
        val sensitiveActionDescription: String,
    ) : AutomationAction()
}

/**
 * A single step in an automation plan.
 */
data class AutomationStep(
    val stepIndex: Int,
    val description: String,
    val action: AutomationAction,
    val safetyRisk: SafetyRiskLevel = SafetyRiskLevel.Safe,
    val maxRetries: Int = 2,
)

/**
 * A validated multi-step plan produced by the task planner.
 */
data class AutomationPlan(
    val originalPrompt: String,
    val steps: List<AutomationStep>,
    val containsConsequentialAction: Boolean = false,
    val sensitiveActionReason: String? = null,
)

/**
 * Result of executing an individual automation step.
 */
data class StepExecutionResult(
    val stepIndex: Int,
    val actionName: String,
    val isSuccess: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Overall outcome of an automation task execution.
 */
data class AutomationExecutionReport(
    val taskPrompt: String,
    val status: AutomationTaskStatus,
    val completedSteps: List<StepExecutionResult>,
    val summary: String,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
