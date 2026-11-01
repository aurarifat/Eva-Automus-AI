package com.evai.rifatcoder.domain.contract

/**
 * Architectural contract for future Android Accessibility Automation.
 *
 * CRITICAL REQUIREMENTS (Future Phase):
 * 1. Must provide real, permission-based Android Accessibility Service functionality.
 * 2. Must inspect the active accessibility window and read node hierarchies.
 * 3. Must find controls by accessible text, resource ID, or content description.
 * 4. Must execute verified node actions (click, scroll, text input, global back).
 * 5. Must require manual user authorization in Android Settings.
 * 6. Must not be simulated with fake buttons or unauthorized injection.
 */
interface AccessibilityAutomationContract {
    enum class ServiceState {
        NotConfigured,
        PermissionRequired,
        DisabledBySystem,
        Connected,
        Active,
        Error,
    }

    data class AutomationNode(
        val text: String?,
        val contentDescription: String?,
        val className: String,
        val isClickable: Boolean,
        val isScrollable: Boolean,
        val isEditable: Boolean,
        val viewIdResourceName: String?,
    )

    data class ActionResult(
        val isSuccess: Boolean,
        val actionName: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
    )

    val currentServiceState: ServiceState

    fun checkServiceAvailability(): ServiceState
}
