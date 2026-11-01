package com.evai.rifatcoder.ui.automation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evai.rifatcoder.data.accessibility.AutomationActionDispatcher
import com.evai.rifatcoder.domain.automation.AutomationTaskPlanner
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for the Automation & App Control Screen.
 */
class AutomationViewModel @JvmOverloads constructor(
    application: Application,
    coordinatorOverride: AutomationCoordinator? = null,
) : AndroidViewModel(application) {

    val coordinator: AutomationCoordinator = coordinatorOverride ?: AutomationCoordinator(
        context = application,
        dispatcher = AutomationActionDispatcher(application),
        planner = AutomationTaskPlanner(),
        coroutineScope = viewModelScope,
    )

    val uiState: StateFlow<AutomationUiState> = coordinator.uiState

    fun refreshServiceStatus() {
        coordinator.refreshServiceStatus()
    }

    fun submitCommand(prompt: String) {
        coordinator.submitCommand(prompt)
    }

    fun cancelActiveTask() {
        coordinator.cancelActiveTask()
    }

    fun confirmPendingTask() {
        coordinator.confirmPendingTask()
    }

    fun openAccessibilitySettings() {
        coordinator.openSettings()
    }
}
