package com.evai.rifatcoder.domain.tool

/**
 * Functional category grouping for AI tools.
 */
enum class ToolCategory(val displayName: String) {
    DEVICE_AND_SYSTEM("Device Status & System Controls"),
    SCREEN_APPS_MEDIA("Screen, Apps & Media"),
    COMMUNICATION("Communication & Contacts"),
    SKILLS_AND_PLUGINS("Skills & Plugins"),
    MAIL("Email Management"),
    TASKS_AND_WORKFLOWS("Tasks, Workflows & Shortcuts"),
    LOCATION_AND_NAV("Location & Navigation"),
    CALENDAR("Calendar & Events"),
    MEMOS("Memos & To-Dos"),
}

/**
 * Result returned by executing an AI Tool.
 */
data class ToolExecutionResult(
    val toolName: String,
    val isSuccess: Boolean,
    val message: String,
    val data: Map<String, Any?> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Definition of an individual AI Tool capability.
 */
data class AiTool(
    val name: String,
    val aliases: List<String>,
    val description: String,
    val category: ToolCategory,
    val isSensitive: Boolean = false,
)
