package com.evai.rifatcoder.ui.automation

import com.evai.rifatcoder.domain.automation.AutomationExecutionReport
import com.evai.rifatcoder.domain.automation.AutomationPlan
import com.evai.rifatcoder.domain.automation.AutomationTaskStatus
import com.evai.rifatcoder.domain.automation.StepExecutionResult

/**
 * UI State for the Automation screen and automation controllers.
 */
data class AutomationUiState(
    val isServiceEnabled: Boolean = false,
    val isServiceConnected: Boolean = false,
    val taskStatus: AutomationTaskStatus = AutomationTaskStatus.Idle,
    val activePrompt: String? = null,
    val activePlan: AutomationPlan? = null,
    val currentStepIndex: Int = 0,
    val currentActionDescription: String? = null,
    val completedStepsCount: Int = 0,
    val totalStepsCount: Int = 0,
    val lastExecutionReport: AutomationExecutionReport? = null,
    val taskHistory: List<AutomationExecutionReport> = emptyList(),
    val pendingConfirmationPrompt: String? = null,
    val pendingConfirmationReason: String? = null,
    val errorMessage: String? = null,
)
