package com.evai.rifatcoder.domain.automation

import java.util.Locale

/**
 * Validates automation plans and steps against strict safety, security, and privacy constraints.
 */
object AutomationSafetyGuard {

    const val MAX_STEPS_PER_PLAN = 10
    const val MAX_RETRIES_PER_STEP = 2
    const val MAX_TASK_TIMEOUT_MS = 45000L

    private val SENSITIVE_KEYWORDS = listOf(
        "pay", "purchase", "buy", "transfer", "checkout",
        "delete", "remove", "erase", "wipe", "uninstall",
        "send sms", "send message", "send email", "publish", "post to",
        "modify security", "change password", "reset settings", "format"
    )

    private val CREDENTIAL_PATTERNS = Regex(
        "\\b(password|passwd|otp|one[- ]?time[- ]?pin|pin code|security code|cvv|cvc|passcode|secret)\\b",
        RegexOption.IGNORE_CASE
    )

    /**
     * Validates an entire proposed AutomationPlan before any action is executed.
     */
    fun validatePlan(plan: AutomationPlan): Result<AutomationPlan> {
        if (plan.steps.isEmpty()) {
            return Result.failure(IllegalArgumentException("Automation plan contains zero steps."))
        }
        if (plan.steps.size > MAX_STEPS_PER_PLAN) {
            return Result.failure(
                IllegalArgumentException("Plan exceeds maximum allowed steps ($MAX_STEPS_PER_PLAN).")
            )
        }

        // Validate each step
        var hasConsequential = false
        var reason: String? = null

        plan.steps.forEachIndexed { index, step ->
            val stepValidation = validateStep(step)
            if (stepValidation.isFailure) {
                return Result.failure(stepValidation.exceptionOrNull()!!)
            }
            if (step.safetyRisk == SafetyRiskLevel.Consequential) {
                hasConsequential = true
                reason = reason ?: "Step ${index + 1} involves consequential action: ${step.description}"
            }
        }

        return Result.success(
            plan.copy(
                containsConsequentialAction = hasConsequential,
                sensitiveActionReason = reason,
            )
        )
    }

    /**
     * Validates a single AutomationStep against the allowlist and safety rules.
     */
    fun validateStep(step: AutomationStep): Result<Unit> {
        val action = step.action

        // Enforce retry bounds
        if (step.maxRetries > MAX_RETRIES_PER_STEP) {
            return Result.failure(
                IllegalArgumentException("Step ${step.stepIndex} exceeds max retries limit ($MAX_RETRIES_PER_STEP).")
            )
        }

        when (action) {
            is AutomationAction.InputText -> {
                // Check if target or input value contains credential patterns
                val target = (action.targetText ?: "") + " " + (action.targetId ?: "")
                if (isCredentialField(target)) {
                    return Result.failure(
                        SecurityException("Automated input into password, PIN, or OTP fields is strictly forbidden.")
                    )
                }
            }
            is AutomationAction.OpenApp -> {
                if (action.appQuery.isBlank()) {
                    return Result.failure(IllegalArgumentException("App query cannot be blank."))
                }
            }
            is AutomationAction.SetAlarm -> {
                if (action.hour !in 0..23 || action.minute !in 0..59) {
                    return Result.failure(IllegalArgumentException("Invalid alarm time: ${action.hour}:${action.minute}"))
                }
            }
            is AutomationAction.WaitTransition -> {
                if (action.durationMs > 10000L) {
                    return Result.failure(IllegalArgumentException("Transition wait cannot exceed 10 seconds."))
                }
            }
            is AutomationAction.InspectScreen,
            is AutomationAction.ClickNode,
            is AutomationAction.HoldNode,
            is AutomationAction.Scroll,
            is AutomationAction.NavigateBack,
            is AutomationAction.NavigateHome,
            is AutomationAction.AdjustVolume,
            is AutomationAction.ToggleFlashlight,
            is AutomationAction.CheckDeviceStatus,
            is AutomationAction.AskConfirmation -> {
                // Permitted allowlist actions
            }
        }

        return Result.success(Unit)
    }

    /**
     * Detects if an AccessibleNode represents a password, OTP, or secret input field.
     */
    fun isSensitiveNode(node: AccessibleNode): Boolean {
        if (node.isPassword) return true
        val textToCheck = listOfNotNull(node.text, node.contentDescription, node.viewIdResourceName)
            .joinToString(" ")
        return isCredentialField(textToCheck)
    }

    /**
     * Checks if a text label represents a credential field.
     */
    fun isCredentialField(text: String): Boolean {
        return CREDENTIAL_PATTERNS.containsMatchIn(text)
    }

    /**
     * Evaluates if a natural language prompt describes a consequential or sensitive operation.
     */
    fun evaluateConsequentialRisk(prompt: String): SafetyRiskLevel {
        val lower = prompt.lowercase(Locale.ROOT)
        for (keyword in SENSITIVE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return SafetyRiskLevel.Consequential
            }
        }
        return SafetyRiskLevel.Safe
    }
}
