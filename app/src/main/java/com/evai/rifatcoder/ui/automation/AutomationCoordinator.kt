package com.evai.rifatcoder.ui.automation

import android.content.Context
import com.evai.rifatcoder.data.accessibility.AutomationActionDispatcher
import com.evai.rifatcoder.data.accessibility.EvaAccessibilityService
import com.evai.rifatcoder.domain.automation.AutomationAction
import com.evai.rifatcoder.domain.automation.AutomationExecutionReport
import com.evai.rifatcoder.domain.automation.AutomationPlan
import com.evai.rifatcoder.domain.automation.AutomationTaskPlanner
import com.evai.rifatcoder.domain.automation.AutomationTaskStatus
import com.evai.rifatcoder.domain.automation.SafetyRiskLevel
import com.evai.rifatcoder.domain.automation.StepExecutionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Central coordinator for executing, monitoring, and cancelling automated device tasks.
 * Shared across UI Screen, Chat Engine, and Voice Assistant.
 */
class AutomationCoordinator(
    private val context: Context,
    private val dispatcher: AutomationActionDispatcher = AutomationActionDispatcher(context),
    private val planner: AutomationTaskPlanner = AutomationTaskPlanner(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
) {

    private val _uiState = MutableStateFlow(AutomationUiState())
    val uiState: StateFlow<AutomationUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var pendingPlanToExecute: AutomationPlan? = null

    init {
        refreshServiceStatus()
    }

    /**
     * Refreshes whether the Android Accessibility Service is enabled and bound.
     */
    fun refreshServiceStatus() {
        val enabled = EvaAccessibilityService.isServiceEnabled(context)
        val connected = EvaAccessibilityService.instance != null
        _uiState.update {
            it.copy(
                isServiceEnabled = enabled,
                isServiceConnected = connected,
            )
        }
    }

    /**
     * Evaluates whether a prompt is an automation command.
     */
    fun isAutomationCommand(prompt: String): Boolean {
        return planner.isAutomationIntent(prompt)
    }

    /**
     * Dispatches an automation command.
     * Can be invoked from typed chat, voice input, or quick actions.
     */
    fun submitCommand(userPrompt: String, skipConfirmation: Boolean = false) {
        refreshServiceStatus()

        _uiState.update {
            it.copy(
                activePrompt = userPrompt,
                taskStatus = AutomationTaskStatus.Planning,
                errorMessage = null,
            )
        }
        com.evai.rifatcoder.data.glow.EdgeGlowStateHolder.onTaskPlanning(userPrompt)

        val planResult = planner.createPlan(userPrompt)
        if (planResult.isFailure) {
            val err = planResult.exceptionOrNull()?.localizedMessage ?: "Invalid automation plan."
            _uiState.update {
                it.copy(
                    taskStatus = AutomationTaskStatus.Failed,
                    errorMessage = err,
                )
            }
            com.evai.rifatcoder.data.glow.EdgeGlowStateHolder.onTaskFailed(err)
            return
        }

        val plan = planResult.getOrThrow()

        // Check if consequential action requires user confirmation
        if (plan.containsConsequentialAction && !skipConfirmation) {
            pendingPlanToExecute = plan
            val reason = plan.sensitiveActionReason ?: "This action has external consequences."
            _uiState.update {
                it.copy(
                    activePlan = plan,
                    taskStatus = AutomationTaskStatus.WaitingForConfirmation,
                    pendingConfirmationPrompt = userPrompt,
                    pendingConfirmationReason = reason,
                )
            }
            com.evai.rifatcoder.data.glow.EdgeGlowStateHolder.onTaskWaitingForConfirmation(userPrompt, reason)
            return
        }

        executePlan(plan)
    }

    /**
     * Executes a confirmed or safe AutomationPlan.
     */
    private fun executePlan(plan: AutomationPlan) {
        activeJob?.cancel()
        activeJob = coroutineScope.launch {
            _uiState.update {
                it.copy(
                    activePlan = plan,
                    taskStatus = AutomationTaskStatus.Running,
                    totalStepsCount = plan.steps.size,
                    completedStepsCount = 0,
                    currentStepIndex = 1,
                    currentActionDescription = plan.steps.firstOrNull()?.description ?: "Starting task...",
                )
            }

            val completed = mutableListOf<StepExecutionResult>()
            var isFailed = false
            var failureMessage: String? = null

            for (step in plan.steps) {
                // Check if user cancelled
                if (_uiState.value.taskStatus == AutomationTaskStatus.Cancelled) {
                    break
                }

                _uiState.update {
                    it.copy(
                        currentStepIndex = step.stepIndex,
                        currentActionDescription = step.description,
                    )
                }
                com.evai.rifatcoder.data.glow.EdgeGlowStateHolder.onTaskExecuting(
                    stepIndex = step.stepIndex,
                    totalSteps = plan.steps.size,
                    description = step.description,
                )

                // Verify service availability for accessibility-dependent actions
                if (step.action is AutomationAction.ClickNode ||
                    step.action is AutomationAction.HoldNode ||
                    step.action is AutomationAction.Scroll ||
                    step.action is AutomationAction.InputText ||
                    step.action is AutomationAction.InspectScreen) {
                    if (EvaAccessibilityService.instance == null) {
                        isFailed = true
                        failureMessage = "Accessibility service was disabled or disconnected. Automation halted for safety."
                        break
                    }
                }

                // Execute action
                var attempt = 0
                var stepResult: StepExecutionResult? = null

                while (attempt <= step.maxRetries) {
                    attempt++
                    stepResult = dispatcher.executeStep(step)
                    if (stepResult.isSuccess) break
                }

                val finalResult = stepResult ?: StepExecutionResult(
                    stepIndex = step.stepIndex,
                    actionName = step.action::class.java.simpleName,
                    isSuccess = false,
                    message = "Execution failed after retries.",
                )

                completed.add(finalResult)
                _uiState.update { it.copy(completedStepsCount = completed.size) }
                com.evai.rifatcoder.data.glow.EdgeGlowStateHolder.onTaskStepCompleted(
                    stepIndex = completed.size,
                    totalSteps = plan.steps.size,
                )

                // Stop if step failed and action was critical
                if (!finalResult.isSuccess) {
                    // Non-fatal if InspectScreen fails after an action
                    if (step.action !is AutomationAction.InspectScreen) {
                        isFailed = true
                        failureMessage = finalResult.message
                        break
                    }
                }
            }

            val finalStatus = when {
                _uiState.value.taskStatus == AutomationTaskStatus.Cancelled -> AutomationTaskStatus.Cancelled
                isFailed -> AutomationTaskStatus.Failed
                else -> AutomationTaskStatus.Completed
            }

            val summaryText = when (finalStatus) {
                AutomationTaskStatus.Completed -> "Successfully completed ${completed.size} automation step(s)."
                AutomationTaskStatus.Cancelled -> "Task was stopped by the user."
                else -> failureMessage ?: "Task encountered an error."
            }

            val report = AutomationExecutionReport(
                taskPrompt = plan.originalPrompt,
                status = finalStatus,
                completedSteps = completed,
                summary = summaryText,
                errorMessage = if (isFailed) failureMessage else null,
            )

            _uiState.update {
                it.copy(
                    taskStatus = finalStatus,
                    lastExecutionReport = report,
                    taskHistory = (listOf(report) + it.taskHistory).take(20),
                    currentActionDescription = summaryText,
                    errorMessage = if (isFailed) failureMessage else null,
                    pendingConfirmationPrompt = null,
                    pendingConfirmationReason = null,
                )
            }

            when (finalStatus) {
                AutomationTaskStatus.Completed -> com.evai.rifatcoder.data.glow.EdgeGlowStateHolder.onTaskSuccess(summaryText)
                AutomationTaskStatus.Failed -> com.evai.rifatcoder.data.glow.EdgeGlowStateHolder.onTaskFailed(summaryText)
                AutomationTaskStatus.Cancelled -> com.evai.rifatcoder.data.glow.EdgeGlowStateHolder.onTaskCancelled()
                else -> Unit
            }
        }
    }

    /**
     * Confirms a pending consequential action and begins execution.
     */
    fun confirmPendingTask() {
        val plan = pendingPlanToExecute ?: return
        pendingPlanToExecute = null
        _uiState.update {
            it.copy(
                pendingConfirmationPrompt = null,
                pendingConfirmationReason = null,
            )
        }
        executePlan(plan)
    }

    /**
     * Cancels a pending or active task.
     */
    fun cancelActiveTask() {
        activeJob?.cancel()
        activeJob = null
        pendingPlanToExecute = null
        com.evai.rifatcoder.data.glow.EdgeGlowStateHolder.onTaskCancelled()
        _uiState.update {
            it.copy(
                taskStatus = AutomationTaskStatus.Cancelled,
                currentActionDescription = "Task cancelled by user.",
                pendingConfirmationPrompt = null,
                pendingConfirmationReason = null,
            )
        }
    }

    /**
     * Clears all automation execution history for privacy.
     */
    fun clearHistory() {
        _uiState.update {
            it.copy(
                taskHistory = emptyList(),
                lastExecutionReport = null,
            )
        }
    }

    /**
     * Launches the system accessibility settings.
     */
    fun openSettings() {
        EvaAccessibilityService.openAccessibilitySettings(context)
    }
}
