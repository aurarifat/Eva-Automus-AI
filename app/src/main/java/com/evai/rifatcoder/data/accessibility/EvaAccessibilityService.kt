package com.evai.rifatcoder.data.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.evai.rifatcoder.domain.automation.AccessibleNode
import com.evai.rifatcoder.domain.automation.AutomationSafetyGuard
import java.util.Locale

/**
 * Official Android AccessibilityService for EVA AI Automation.
 *
 * Provides real, user-authorized interaction capabilities:
 * - Active window hierarchy inspection
 * - Node clicking and holding
 * - Scrolling
 * - Text entry (guarded against credentials)
 * - Global navigation (Back, Home, Recents, Notifications)
 */
class EvaAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: EvaAccessibilityService? = null
            private set

        /**
         * Checks whether EvaAccessibilityService is currently enabled in Android Accessibility Settings.
         */
        fun isServiceEnabled(context: Context): Boolean {
            if (instance != null) return true
            val expectedServiceName = "${context.packageName}/${EvaAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            return enabledServices.split(':').any {
                it.equals(expectedServiceName, ignoreCase = true) ||
                it.contains(EvaAccessibilityService::class.java.simpleName, ignoreCase = true)
            }
        }

        /**
         * Opens the Android Accessibility Settings screen for explicit user authorization.
         */
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private val inspector = AccessibilityNodeInspector()
    var currentPackageName: String? = null
        private set

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = info.flags or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        info.notificationTimeout = 100
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.let {
                currentPackageName = it.toString()
            }
        }
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    /**
     * Inspects the active root window and returns the node hierarchy snapshot.
     */
    fun inspectCurrentWindow(): AccessibilityNodeInspector.InspectionResult {
        val root = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        }
        return inspector.inspectWindow(root)
    }

    /**
     * Finds and clicks a target node in the active window.
     * If the target is not directly clickable, searches up the parent hierarchy.
     */
    fun findAndClickNode(
        targetText: String? = null,
        targetId: String? = null,
        targetDesc: String? = null,
    ): Boolean {
        val root = rootInActiveWindow ?: return false
        val candidate = locateNode(root, targetText, targetId, targetDesc) ?: return false

        // Attempt click on candidate or its clickable parent
        var current: AccessibilityNodeInfo? = candidate
        while (current != null) {
            if (current.isClickable) {
                val success = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (success) return true
            }
            current = current.parent
        }

        return candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /**
     * Finds and long clicks / holds a target node in the active window.
     */
    fun findAndHoldNode(
        targetText: String? = null,
        targetId: String? = null,
        targetDesc: String? = null,
    ): Boolean {
        val root = rootInActiveWindow ?: return false
        val candidate = locateNode(root, targetText, targetId, targetDesc) ?: return false

        var current: AccessibilityNodeInfo? = candidate
        while (current != null) {
            if (current.isLongClickable) {
                val success = current.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                if (success) return true
            }
            current = current.parent
        }

        return candidate.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    /**
     * Enters text into an editable node, strictly verifying it is not a credential field.
     */
    fun inputText(
        targetText: String? = null,
        targetId: String? = null,
        textToInput: String,
    ): Boolean {
        val root = rootInActiveWindow ?: return false

        // Find editable field
        val candidate = locateNode(root, targetText, targetId, null) ?: findFirstEditableNode(root) ?: return false

        // Verify not sensitive/password
        if (candidate.isPassword) {
            return false
        }
        val textToCheck = listOfNotNull(candidate.text, candidate.contentDescription, candidate.viewIdResourceName)
            .joinToString(" ")
        if (AutomationSafetyGuard.isCredentialField(textToCheck)) {
            return false
        }

        // Focus and set text
        candidate.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToInput)
        }
        return candidate.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    /**
     * Scrolls the first scrollable node in the active window.
     */
    fun scrollActiveWindow(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findFirstScrollableNode(root) ?: return false

        val action = if (forward) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
        return scrollable.performAction(action)
    }

    /**
     * Dispatches global navigation actions (Back, Home, Recents, Notifications).
     */
    fun performGlobal(globalAction: Int): Boolean {
        return performGlobalAction(globalAction)
    }

    private fun locateNode(
        root: AccessibilityNodeInfo,
        text: String?,
        id: String?,
        desc: String?,
    ): AccessibilityNodeInfo? {
        // Try by ID first
        if (!id.isNullOrBlank()) {
            val list = root.findAccessibilityNodeInfosByViewId(id)
            if (list.isNotEmpty()) return list.first()
        }

        // Try by text
        if (!text.isNullOrBlank()) {
            val list = root.findAccessibilityNodeInfosByText(text)
            if (list.isNotEmpty()) return list.first()
        }

        // Search recursively for description or partial text
        return searchRecursive(root, text?.lowercase(Locale.ROOT), desc?.lowercase(Locale.ROOT))
    }

    private fun searchRecursive(
        node: AccessibilityNodeInfo,
        textLower: String?,
        descLower: String?,
    ): AccessibilityNodeInfo? {
        val nText = node.text?.toString()?.lowercase(Locale.ROOT)
        val nDesc = node.contentDescription?.toString()?.lowercase(Locale.ROOT)

        if (textLower != null && nText?.contains(textLower) == true) return node
        if (descLower != null && nDesc?.contains(descLower) == true) return node

        for (i in 0 until node.childCount) {
            val child = try {
                node.getChild(i)
            } catch (_: Exception) {
                null
            }
            if (child != null) {
                val match = searchRecursive(child, textLower, descLower)
                if (match != null) return match
            }
        }
        return null
    }

    private fun findFirstScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = try {
                node.getChild(i)
            } catch (_: Exception) {
                null
            }
            if (child != null) {
                val found = findFirstScrollableNode(child)
                if (found != null) return found
            }
        }
        return null
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = try {
                node.getChild(i)
            } catch (_: Exception) {
                null
            }
            if (child != null) {
                val found = findFirstEditableNode(child)
                if (found != null) return found
            }
        }
        return null
    }
}
