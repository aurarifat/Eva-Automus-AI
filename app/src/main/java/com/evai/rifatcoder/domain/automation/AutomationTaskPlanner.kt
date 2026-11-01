package com.evai.rifatcoder.domain.automation

import java.util.Locale

/**
 * Converts user natural-language commands into validated, bounded automation action plans.
 */
class AutomationTaskPlanner {

    /**
     * Determines whether a given user message represents an actionable device automation command.
     */
    fun isAutomationIntent(userPrompt: String): Boolean {
        val trimmed = userPrompt.trim().lowercase(Locale.ROOT)
        if (trimmed.isBlank()) return false

        val triggers = listOf(
            "open ", "launch ", "start ", "go to ",
            "scroll down", "scroll up", "scroll",
            "go back", "back", "navigate back",
            "go home", "home screen", "return home",
            "volume up", "volume down", "turn up volume", "turn down volume", "mute", "unmute",
            "set alarm", "set an alarm",
            "flashlight", "turn on flashlight", "turn off flashlight", "toggle flashlight",
            "battery status", "check battery", "device status",
            "inspect screen", "what controls are available", "read screen", "read the screen",
            "tap ", "click ", "press ", "find ", "search for "
        )

        return triggers.any { trigger ->
            trimmed.startsWith(trigger) || trimmed.contains(trigger)
        }
    }

    /**
     * Creates a bounded action plan from the natural language prompt.
     */
    fun createPlan(userPrompt: String): Result<AutomationPlan> {
        val cleanPrompt = userPrompt.trim()
        if (cleanPrompt.isBlank()) {
            return Result.failure(IllegalArgumentException("Command cannot be blank or empty."))
        }
        val lower = cleanPrompt.lowercase(Locale.ROOT)

        val steps = mutableListOf<AutomationStep>()
        var stepIndex = 1

        // 1. Check for compound command: "Open <app> and search for <query>"
        val compoundSearchRegex = Regex("open\\s+([a-zA-Z0-9 ]+?)\\s+and\\s+search\\s+for\\s+(.+)", RegexOption.IGNORE_CASE)
        val compoundMatch = compoundSearchRegex.find(cleanPrompt)

        if (compoundMatch != null) {
            val appTarget = compoundMatch.groupValues[1].trim()
            val searchQuery = compoundMatch.groupValues[2].trim()

            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Launch $appTarget application",
                    action = AutomationAction.OpenApp(appQuery = appTarget),
                )
            )
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Wait for $appTarget interface to transition",
                    action = AutomationAction.WaitTransition(durationMs = 1500L),
                )
            )
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Inspect active screen for search control",
                    action = AutomationAction.InspectScreen,
                )
            )
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Tap search button or search bar",
                    action = AutomationAction.ClickNode(targetText = "Search", targetDescription = "Search"),
                )
            )
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Input query \"$searchQuery\"",
                    action = AutomationAction.InputText(textToInput = searchQuery),
                )
            )
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Inspect updated search results",
                    action = AutomationAction.InspectScreen,
                )
            )
        }
        // 2. Open an app: "Open YouTube", "Launch Settings", "Go to Chrome"
        else if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ")) {
            val appQuery = cleanPrompt.substringAfter(' ').trim()
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Launch $appQuery application",
                    action = AutomationAction.OpenApp(appQuery = appQuery),
                )
            )
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Verify application window opened",
                    action = AutomationAction.InspectScreen,
                )
            )
        }
        // 3. Navigation actions: Go back, Go home
        else if (lower.contains("go back") || lower == "back" || lower == "navigate back") {
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Navigate backward",
                    action = AutomationAction.NavigateBack,
                )
            )
        }
        else if (lower.contains("go home") || lower.contains("home screen") || lower == "home") {
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Return to Home screen",
                    action = AutomationAction.NavigateHome,
                )
            )
        }
        // 4. Scroll actions
        else if (lower.contains("scroll down")) {
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Scroll active window content down",
                    action = AutomationAction.Scroll(direction = ScrollDirection.FORWARD),
                )
            )
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Inspect updated view hierarchy",
                    action = AutomationAction.InspectScreen,
                )
            )
        }
        else if (lower.contains("scroll up")) {
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Scroll active window content up",
                    action = AutomationAction.Scroll(direction = ScrollDirection.BACKWARD),
                )
            )
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Inspect updated view hierarchy",
                    action = AutomationAction.InspectScreen,
                )
            )
        }
        // 5. Volume controls
        else if (lower.contains("volume up") || lower.contains("turn up volume") || lower.contains("increase volume")) {
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Increase media volume",
                    action = AutomationAction.AdjustVolume(direction = VolumeDirection.UP),
                )
            )
        }
        else if (lower.contains("volume down") || lower.contains("turn down volume") || lower.contains("decrease volume")) {
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Decrease media volume",
                    action = AutomationAction.AdjustVolume(direction = VolumeDirection.DOWN),
                )
            )
        }
        else if (lower.contains("mute")) {
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Mute audio volume",
                    action = AutomationAction.AdjustVolume(direction = VolumeDirection.MUTE),
                )
            )
        }
        // 6. Flashlight
        else if (lower.contains("flashlight")) {
            val turnOff = lower.contains("off") || lower.contains("disable")
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = if (turnOff) "Turn off device flashlight" else "Turn on device flashlight",
                    action = AutomationAction.ToggleFlashlight(enabled = !turnOff),
                )
            )
        }
        // 7. Alarm clock: "Set alarm for 7:30 AM", "Set alarm for 08:00"
        else if (lower.contains("alarm")) {
            val timeRegex = Regex("(\\d{1,2})[:.](\\d{2})\\s*(am|pm)?", RegexOption.IGNORE_CASE)
            val match = timeRegex.find(cleanPrompt)
            val hourRaw = match?.groupValues?.get(1)?.toIntOrNull() ?: 8
            val minRaw = match?.groupValues?.get(2)?.toIntOrNull() ?: 0
            val amPm = match?.groupValues?.get(3)?.lowercase(Locale.ROOT)

            val hour = when {
                amPm == "pm" && hourRaw < 12 -> hourRaw + 12
                amPm == "am" && hourRaw == 12 -> 0
                else -> hourRaw
            }

            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Set system alarm for %02d:%02d".format(hour, minRaw),
                    action = AutomationAction.SetAlarm(hour = hour, minute = minRaw, message = "EVA AI Alarm"),
                )
            )
        }
        // 8. Device status checks
        else if (lower.contains("battery")) {
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Check battery level and charging status",
                    action = AutomationAction.CheckDeviceStatus(DeviceStatusType.BATTERY),
                )
            )
        }
        // 9. Screen inspection / "What controls are available"
        else if (lower.contains("read screen") || lower.contains("inspect") || lower.contains("controls are available")) {
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Inspect active window accessibility tree",
                    action = AutomationAction.InspectScreen,
                )
            )
        }
        // 10. Tap on control: "Tap the Search button", "Click Play", "Find the Search button and tap it"
        else if (lower.startsWith("tap ") || lower.startsWith("click ") || lower.startsWith("press ") || lower.contains("and tap it")) {
            val targetLabel = cleanPrompt
                .replace(Regex("^(tap|click|press|find)\\s+(the\\s+)?", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s+and\\s+tap\\s+it.*$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s+(button|icon|tab|control)$", RegexOption.IGNORE_CASE), "")
                .trim()

            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Find and tap \"$targetLabel\"",
                    action = AutomationAction.ClickNode(targetText = targetLabel),
                )
            )
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Verify action outcome on screen",
                    action = AutomationAction.InspectScreen,
                )
            )
        }
        // Fallback: Default to inspecting screen and attempting target resolution
        else {
            steps.add(
                AutomationStep(
                    stepIndex = stepIndex++,
                    description = "Inspect screen for \"$cleanPrompt\"",
                    action = AutomationAction.InspectScreen,
                )
            )
        }

        // Assess consequential risk
        val risk = AutomationSafetyGuard.evaluateConsequentialRisk(cleanPrompt)
        val initialPlan = AutomationPlan(
            originalPrompt = cleanPrompt,
            steps = steps,
            containsConsequentialAction = risk == SafetyRiskLevel.Consequential,
            sensitiveActionReason = if (risk == SafetyRiskLevel.Consequential) "Command involves sensitive actions" else null,
        )

        return AutomationSafetyGuard.validatePlan(initialPlan)
    }
}
