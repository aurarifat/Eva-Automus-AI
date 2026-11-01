package com.evai.rifatcoder.domain.tool

import android.content.Context
import com.evai.rifatcoder.data.glow.EdgeGlowStateHolder
import com.evai.rifatcoder.data.tool.RealAiToolExecutor
import java.util.Locale

/**
 * Central AI Tool Registry and Execution Manager (Phase 15).
 * Bridges user voice and chat prompts with genuine device tool functions.
 */
class AiToolRegistry(
    private val context: Context,
    private val executor: RealAiToolExecutor = RealAiToolExecutor(context),
) {

    private val tools: List<AiTool> = listOf(
        // Section A: Device Status & System Controls
        AiTool("check battery status", listOf("battery status", "battery", "check battery"), "Inspect device battery percentage and charging status", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("device health", listOf("device health", "system health"), "Audit RAM, thermal and memory health", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("device_memory_status", listOf("memory status", "ram status", "check ram"), "Check available and total device RAM", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("device_storage_status", listOf("storage status", "disk space", "check storage"), "Inspect free and total disk storage", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("device_network_status", listOf("network status", "internet status", "check wifi connection"), "Inspect current network connectivity", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("check date and time", listOf("date and time", "what time is it", "current time"), "Get current formatted date and time", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("flashlight_toggle", listOf("flashlight", "turn on flashlight", "turn off flashlight", "toggle flashlight"), "Toggle hardware camera torch", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("alarm_set_alarm", listOf("set alarm", "set an alarm", "wake me up"), "Schedule system clock alarm", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("alarm_modify_alarm", listOf("modify alarm", "change alarm", "open alarms"), "Open alarm clock manager", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("volume_adjust_stream", listOf("volume up", "volume down", "turn up volume", "turn down volume"), "Adjust system audio volume", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("volume_set_ringer_mode", listOf("mute", "silent mode", "vibrate mode"), "Adjust ringer mode", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("bluetooth_state", listOf("bluetooth status", "check bluetooth", "bluetooth"), "Check Bluetooth adapter status and paired devices", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("open_bluetooth", listOf("open bluetooth", "turn on bluetooth"), "Open Bluetooth settings", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("wifi_status", listOf("wifi status", "check wifi"), "Inspect Wi-Fi status", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("wifi_set_enabled", listOf("open wifi", "wifi settings"), "Open Wi-Fi settings", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("dnd_open_policy_access_settings", listOf("dnd settings", "do not disturb access"), "Open DND access settings", ToolCategory.DEVICE_AND_SYSTEM),
        AiTool("system_settings_set_brightness", listOf("brightness", "change brightness"), "Open system display brightness settings", ToolCategory.DEVICE_AND_SYSTEM),

        // Section B: Screen, Apps & Media
        AiTool("read the screen", listOf("read screen", "inspect screen", "what is on my screen", "read the screen"), "Extract text and interactive controls from active screen", ToolCategory.SCREEN_APPS_MEDIA),
        AiTool("launch_app", listOf("open ", "launch ", "start "), "Launch an installed application", ToolCategory.SCREEN_APPS_MEDIA),
        AiTool("take a screenshot", listOf("screenshot", "take screenshot", "capture screen"), "Capture screen image", ToolCategory.SCREEN_APPS_MEDIA),
        AiTool("open a photo", listOf("open photo", "open gallery", "view photos"), "Open system photo viewer", ToolCategory.SCREEN_APPS_MEDIA),
        AiTool("take a photo", listOf("take photo", "open camera", "camera"), "Launch camera capture", ToolCategory.SCREEN_APPS_MEDIA),
        AiTool("play_store_check_app", listOf("install app", "play store", "download app"), "Open Play Store for target application", ToolCategory.SCREEN_APPS_MEDIA),

        // Section C: Communication & Contacts
        AiTool("phone_dial", listOf("call ", "dial "), "Open phone dialer with number", ToolCategory.COMMUNICATION),
        AiTool("send_to_sms_message", listOf("send sms", "send text", "text message"), "Compose SMS message", ToolCategory.COMMUNICATION),
        AiTool("contacts_list", listOf("contacts", "list contacts", "find contact"), "Audit system contacts", ToolCategory.COMMUNICATION),

        // Section D: Skills & Plugins
        AiTool("list_skills", listOf("skills", "list skills", "plugins"), "List active AI skills", ToolCategory.SKILLS_AND_PLUGINS),
        AiTool("add_skill", listOf("add skill", "install skill"), "Register a custom skill", ToolCategory.SKILLS_AND_PLUGINS),

        // Section E: Mail
        AiTool("send email", listOf("send email", "email", "compose email"), "Open email composer", ToolCategory.MAIL),

        // Section F: Tasks, Workflows & Shortcuts
        AiTool("workflow_list", listOf("workflows", "list workflows"), "List automated workflows", ToolCategory.TASKS_AND_WORKFLOWS),
        AiTool("plan the next steps", listOf("plan next steps", "plan steps"), "Generate plan for multi-step tasks", ToolCategory.TASKS_AND_WORKFLOWS),

        // Section G: Location & Navigation
        AiTool("get location", listOf("location", "where am i", "my location"), "Check location provider", ToolCategory.LOCATION_AND_NAV),
        AiTool("map_navigate", listOf("navigate to", "directions to", "map to"), "Launch navigation map", ToolCategory.LOCATION_AND_NAV),

        // Section H: Calendar
        AiTool("create a calendar event", listOf("create event", "calendar event", "schedule meeting"), "Add new calendar event", ToolCategory.CALENDAR),

        // Section I: Memos & To-Dos
        AiTool("add to-do", listOf("add todo", "add to-do", "remind me to", "todo add"), "Save a to-do item", ToolCategory.MEMOS),
        AiTool("view to-dos", listOf("view todos", "my todos", "todo list", "show todos"), "List saved to-dos", ToolCategory.MEMOS),
        AiTool("delete to-do", listOf("delete todo", "clear todo"), "Remove a to-do item", ToolCategory.MEMOS),
    )

    fun getAllTools(): List<AiTool> = tools

    fun findMatchingTool(prompt: String): AiTool? {
        val lower = prompt.trim().lowercase(Locale.ROOT)
        return tools.find { tool ->
            tool.name.lowercase(Locale.ROOT) == lower || tool.aliases.any { alias ->
                lower.startsWith(alias) || lower.contains(alias)
            }
        }
    }

    suspend fun executeTool(tool: AiTool, params: Map<String, String> = emptyMap()): ToolExecutionResult {
        EdgeGlowStateHolder.onTaskExecuting(1, 1, tool.description)
        val result = executor.execute(tool.name, params)
        if (result.isSuccess) {
            EdgeGlowStateHolder.onTaskSuccess(result.message)
        } else {
            EdgeGlowStateHolder.onTaskFailed(result.message)
        }
        return result
    }
}
