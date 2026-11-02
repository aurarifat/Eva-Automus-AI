package com.evai.rifatcoder.data.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.evai.rifatcoder.domain.automation.AccessibleNode
import com.evai.rifatcoder.domain.automation.AutomationSafetyGuard
import java.util.Locale

/**
 * Safely inspects active accessibility window hierarchies with bounded depth and node limits.
 */
class AccessibilityNodeInspector {

    companion object {
        const val MAX_TRAVERSAL_DEPTH = 15
        const val MAX_NODE_COUNT = 150
    }

    data class InspectionResult(
        val packageName: String?,
        val rootNode: AccessibleNode?,
        val allNodes: List<AccessibleNode>,
        val totalNodeCount: Int,
        val isTruncated: Boolean,
        val errorMessage: String? = null,
    )

    /**
     * Inspects the provided root AccessibilityNodeInfo and produces a sanitized, flat snapshot.
     */
    fun inspectWindow(rootNodeInfo: AccessibilityNodeInfo?): InspectionResult {
        if (rootNodeInfo == null) {
            return InspectionResult(
                packageName = null,
                rootNode = null,
                allNodes = emptyList(),
                totalNodeCount = 0,
                isTruncated = false,
                errorMessage = "Active window has no accessible root node."
            )
        }

        val collected = mutableListOf<AccessibleNode>()
        val packageName = rootNodeInfo.packageName?.toString()

        val rootNode = extractNodeData(rootNodeInfo, depth = 0)
        traverse(rootNodeInfo, currentDepth = 0, collected = collected)

        return InspectionResult(
            packageName = packageName,
            rootNode = rootNode,
            allNodes = collected,
            totalNodeCount = collected.size,
            isTruncated = collected.size >= MAX_NODE_COUNT,
            errorMessage = null,
        )
    }

    private fun traverse(
        info: AccessibilityNodeInfo?,
        currentDepth: Int,
        collected: MutableList<AccessibleNode>,
    ) {
        if (info == null || currentDepth > MAX_TRAVERSAL_DEPTH || collected.size >= MAX_NODE_COUNT) {
            return
        }

        val nodeData = extractNodeData(info, currentDepth)
        collected.add(nodeData)

        val childCount = info.childCount
        for (i in 0 until childCount) {
            if (collected.size >= MAX_NODE_COUNT) break
            val child = try {
                info.getChild(i)
            } catch (e: Exception) {
                null
            }
            if (child != null) {
                traverse(child, currentDepth + 1, collected)
            }
        }
    }

    private fun extractNodeData(info: AccessibilityNodeInfo, depth: Int): AccessibleNode {
        val bounds = Rect()
        info.getBoundsInScreen(bounds)

        val isPass = try {
            info.isPassword
        } catch (_: Exception) {
            false
        }

        val isSensitive = isPass ||
            AutomationSafetyGuard.isCredentialField(info.viewIdResourceName ?: "") ||
            AutomationSafetyGuard.isCredentialField(info.contentDescription?.toString() ?: "")

        // Strictly protect credentials: never read, expose, or extract text from password fields
        val rawText = if (isSensitive) null else info.text?.toString()
        val rawContentDesc = if (isSensitive) "Protected Credential Field" else info.contentDescription?.toString()

        return AccessibleNode(
            id = "${info.viewIdResourceName ?: "node"}_d${depth}_b${bounds.left}_${bounds.top}",
            text = rawText,
            contentDescription = rawContentDesc,
            className = info.className?.toString() ?: "android.view.View",
            bounds = bounds,
            isClickable = info.isClickable,
            isScrollable = info.isScrollable,
            isEditable = info.isEditable,
            isPassword = isSensitive,
            isEnabled = info.isEnabled,
            isFocused = info.isFocused,
            viewIdResourceName = info.viewIdResourceName,
        )
    }

    /**
     * Finds nodes matching a target query in text, content description, or view ID.
     */
    fun findMatchingNodes(
        nodes: List<AccessibleNode>,
        query: String?,
        requireClickable: Boolean = false,
    ): List<AccessibleNode> {
        if (query.isNullOrBlank()) return emptyList()
        val cleanQuery = query.trim().lowercase(Locale.ROOT)

        return nodes.filter { node ->
            if (requireClickable && !node.isClickable) return@filter false

            val textMatch = node.text?.lowercase(Locale.ROOT)?.contains(cleanQuery) == true
            val descMatch = node.contentDescription?.lowercase(Locale.ROOT)?.contains(cleanQuery) == true
            val idMatch = node.viewIdResourceName?.lowercase(Locale.ROOT)?.contains(cleanQuery) == true

            textMatch || descMatch || idMatch
        }
    }
}
