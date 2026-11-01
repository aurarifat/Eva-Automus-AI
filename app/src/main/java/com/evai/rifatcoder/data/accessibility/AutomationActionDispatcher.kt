package com.evai.rifatcoder.data.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import com.evai.rifatcoder.domain.automation.AutomationAction
import com.evai.rifatcoder.domain.automation.AutomationStep
import com.evai.rifatcoder.domain.automation.ScrollDirection
import com.evai.rifatcoder.domain.automation.StepExecutionResult
import kotlinx.coroutines.delay

/**
 * Dedicated automation action dispatcher.
 *
 * Executes verified actions through official Android Accessibility and System APIs.
 */
class AutomationActionDispatcher(
    private val context: Context,
    private val appLauncherManager: AppLauncherManager = AppLauncherManager(context),
    private val deviceControlManager: DeviceControlManager = DeviceControlManager(context),
) {

    /**
     * Checks if the required Accessibility Service is currently active.
     */
    fun isAccessibilityActive(): Boolean {
        return EvaAccessibilityService.isServiceEnabled(context) && EvaAccessibilityService.instance != null
    }

    /**
     * Executes an individual automation step and returns a verified result.
     */
    suspend fun executeStep(step: AutomationStep): StepExecutionResult {
        val action = step.action

        return when (action) {
            is AutomationAction.OpenApp -> {
                val launchResult = appLauncherManager.launchApp(action.appQuery, action.packageName)
                StepExecutionResult(
                    stepIndex = step.stepIndex,
                    actionName = "OpenApp",
                    isSuccess = launchResult.isSuccess,
                    message = launchResult.message,
                )
            }

            is AutomationAction.InspectScreen -> {
                val service = EvaAccessibilityService.instance
                if (service == null) {
                    StepExecutionResult(
                        stepIndex = step.stepIndex,
                        actionName = "InspectScreen",
                        isSuccess = false,
                        message = "Accessibility Service is not running. Enable it in Android Settings to read screens.",
                    )
                } else {
                    val inspection = service.inspectCurrentWindow()
                    val count = inspection.totalNodeCount
                    val appName = inspection.packageName ?: "Current window"
                    StepExecutionResult(
                        stepIndex = step.stepIndex,
                        actionName = "InspectScreen",
                        isSuccess = inspection.errorMessage == null,
                        message = if (inspection.errorMessage != null) {
                            inspection.errorMessage
                        } else {
                            "Inspected $appName: $count interactive controls identified."
                        },
                    )
                }
            }

            is AutomationAction.ClickNode -> {
                val service = EvaAccessibilityService.instance
                if (service == null) {
                    StepExecutionResult(
                        stepIndex = step.stepIndex,
                        actionName = "ClickNode",
                        isSuccess = false,
                        message = "Accessibility Service not running. Tap action aborted.",
                    )
                } else {
                    val clicked = service.findAndClickNode(
                        targetText = action.targetText,
                        targetId = action.targetId,
                        targetDesc = action.targetDescription,
                    )
                    StepExecutionResult(
                        stepIndex = step.stepIndex,
                        actionName = "ClickNode",
                        isSuccess = clicked,
                        message = if (clicked) {
                            "Tapped control \"${action.targetText ?: action.targetDescription ?: action.targetId}\"."
                        } else {
                            "Control \"${action.targetText ?: action.targetDescription ?: action.targetId}\" was not found in active window."
                        },
                    )
                }
            }

            is AutomationAction.HoldNode -> {
                val service = EvaAccessibilityService.instance
                if (service == null) {
                    StepExecutionResult(
                        stepIndex = step.stepIndex,
                        actionName = "HoldNode",
                        isSuccess = false,
                        message = "Accessibility Service not running. Long click aborted.",
                    )
                } else {
                    val held = service.findAndHoldNode(
                        targetText = action.targetText,
                        targetId = action.targetId,
                        targetDesc = action.targetDescription,
                    )
                    StepExecutionResult(
                        stepIndex = step.stepIndex,
                        actionName = "HoldNode",
                        isSuccess = held,
                        message = if (held) {
                            "Held control \"${action.targetText ?: action.targetDescription ?: action.targetId}\"."
                        } else {
                            "Target control not found or does not support long press."
                        },
                    )
                }
            }

            is AutomationAction.InputText -> {
                val service = EvaAccessibilityService.instance
                if (service == null) {
                    StepExecutionResult(
                        stepIndex = step.stepIndex,
                        actionName = "InputText",
                        isSuccess = false,
                        message = "Accessibility Service not running. Text entry aborted.",
                    )
                } else {
                    val entered = service.inputText(
                        targetText = action.targetText,
                        targetId = action.targetId,
                        textToInput = action.textToInput,
                    )
                    StepExecutionResult(
                        stepIndex = step.stepIndex,
                        actionName = "InputText",
                        isSuccess = entered,
                        message = if (entered) {
                            "Entered \"${action.textToInput}\" into active input field."
                        } else {
                            "No editable input field found or field is restricted (e.g. password/credentials)."
                        },
                    )
                }
            }

            is AutomationAction.Scroll -> {
                val service = EvaAccessibilityService.instance
                if (service == null) {
                    StepExecutionResult(
                        stepIndex = step.stepIndex,
                        actionName = "Scroll",
                        isSuccess = false,
                        message = "Accessibility Service not running. Scroll aborted.",
                    )
                } else {
                    val forward = action.direction == ScrollDirection.FORWARD
                    val scrolled = service.scrollActiveWindow(forward)
                    StepExecutionResult(
                        stepIndex = step.stepIndex,
                        actionName = "Scroll",
                        isSuccess = scrolled,
                        message = if (scrolled) {
                            "Scrolled window ${if (forward) "down" else "up"}."
                        } else {
                            "No scrollable view container found in active window."
                        },
                    )
                }
            }

            is AutomationAction.NavigateBack -> {
                val service = EvaAccessibilityService.instance
                if (service == null) {
                    StepExecutionResult(
                        stepIndex = step.stepIndex,
                        actionName = "NavigateBack",
                        isSuccess = false,
                        message = "Accessibility Service not running. Back navigation aborted.",
                    )
                } else {
                    val navigated = service.performGlobal(AccessibilityService.GLOBAL_ACTION_BACK)
                    StepExecutionResult(
                        stepIndex = step.stepIndex,
                        actionName = "NavigateBack",
                        isSuccess = navigated,
                        message = if (navigated) "Navigated back." else "Global back navigation failed.",
                    )
                }
            }

            is AutomationAction.NavigateHome -> {
                val service = EvaAccessibilityService.instance
                if (service == null) {
                    StepExecutionResult(
                        stepIndex = step.stepIndex,
                        actionName = "NavigateHome",
                        isSuccess = false,
                        message = "Accessibility Service not running. Home navigation aborted.",
                    )
                } else {
                    val navigated = service.performGlobal(AccessibilityService.GLOBAL_ACTION_HOME)
                    StepExecutionResult(
                        stepIndex = step.stepIndex,
                        actionName = "NavigateHome",
                        isSuccess = navigated,
                        message = if (navigated) "Returned to Home screen." else "Global Home navigation failed.",
                    )
                }
            }

            is AutomationAction.AdjustVolume -> {
                val resultMsg = deviceControlManager.adjustVolume(action.direction, action.streamType)
                StepExecutionResult(
                    stepIndex = step.stepIndex,
                    actionName = "AdjustVolume",
                    isSuccess = !resultMsg.startsWith("Failed"),
                    message = resultMsg,
                )
            }

            is AutomationAction.SetAlarm -> {
                val resultMsg = deviceControlManager.setAlarm(action.hour, action.minute, action.message)
                StepExecutionResult(
                    stepIndex = step.stepIndex,
                    actionName = "SetAlarm",
                    isSuccess = !resultMsg.startsWith("Failed"),
                    message = resultMsg,
                )
            }

            is AutomationAction.ToggleFlashlight -> {
                val resultMsg = deviceControlManager.toggleFlashlight(action.enabled)
                StepExecutionResult(
                    stepIndex = step.stepIndex,
                    actionName = "ToggleFlashlight",
                    isSuccess = !resultMsg.startsWith("Failed"),
                    message = resultMsg,
                )
            }

            is AutomationAction.CheckDeviceStatus -> {
                val resultMsg = deviceControlManager.getDeviceStatus(action.statusType)
                StepExecutionResult(
                    stepIndex = step.stepIndex,
                    actionName = "CheckDeviceStatus",
                    isSuccess = true,
                    message = resultMsg,
                )
            }

            is AutomationAction.WaitTransition -> {
                delay(action.durationMs)
                StepExecutionResult(
                    stepIndex = step.stepIndex,
                    actionName = "WaitTransition",
                    isSuccess = true,
                    message = "Waited ${action.durationMs}ms for UI transition.",
                )
            }

            is AutomationAction.AskConfirmation -> {
                // Pre-handled by coordinator
                StepExecutionResult(
                    stepIndex = step.stepIndex,
                    actionName = "AskConfirmation",
                    isSuccess = true,
                    message = "User confirmed: ${action.sensitiveActionDescription}",
                )
            }
        }
    }
}
