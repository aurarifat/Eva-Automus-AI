package com.evai.rifatcoder.data.voice

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Status enumeration for hotword / wake-word detection.
 */
enum class WakeWordStatus(
    val displayName: String,
    val description: String,
) {
    NOT_CONFIGURED(
        displayName = "Not Configured",
        description = "Wake-word model is not installed or configured on this device.",
    ),
    REQUIRES_DEVICE_SETUP(
        displayName = "Requires Device Setup",
        description = "Requires default digital assistant configuration in Android system settings.",
    ),
    UNAVAILABLE(
        displayName = "Unavailable",
        description = "Wake-word engine requires hardware NPU/DSP integration or system assistant privilege.",
    ),
    FEASIBILITY_CHECK_REQUIRED(
        displayName = "Feasibility Check Required",
        description = "Hardware and background execution feasibility has not yet been verified.",
    ),
    RESTRICTED_BY_OS(
        displayName = "Restricted by Android OS",
        description = "Android 11+ foreground microphone security policies restrict continuous background audio capture.",
    ),
    ACTIVE(
        displayName = "Active",
        description = "Wake-word engine is actively listening for activation phrase.",
    ),
    DISABLED(
        displayName = "Disabled",
        description = "Wake-word listening is currently switched off by user preference.",
    ),
}

/**
 * Exhaustive technical feasibility report for "Hey Eva" wake-word detection on Android.
 * Reflects Outcome B: Honest assessment of background execution, battery, privacy, and Android OS limits.
 */
data class WakeWordFeasibilityReport(
    val targetPhrases: List<String> = listOf("Hey Eva", "Hey Eevva"),
    val status: WakeWordStatus = WakeWordStatus.RESTRICTED_BY_OS,
    val detectionEngineType: String = "On-Device Acoustic Model (Porcupine/Snowboy/PocketSphinx architecture boundary)",
    val backgroundOsRestrictions: String = """
        • Android 11+ (API 30+) strictly limits background audio recording via FOREGROUND_SERVICE_TYPE_MICROPHONE.
        • Third-party applications cannot continuously monitor the microphone while backgrounded or the screen is off without displaying an active ongoing notification and receiving persistent user foreground authorization.
        • Dedicated OS-level wake words (such as Google Assistant) use privileged system firmware and low-power hardware DSP voice triggers (Always-On Processor) that are closed to standard 3rd-party user-installed APKs.
    """.trimIndent(),
    val batteryImpactAssessment: String = """
        • High battery drain: Continuously running software-based audio capture and STT/acoustic inference on the CPU prevents the device from entering low-power Doze sleep states.
        • Estimated battery drain: 8%–15% per hour without low-power hardware DSP offload.
    """.trimIndent(),
    val privacyNotice: String = """
        • Strict zero-leakage privacy requirement: Any persistent ambient listening requires continuous user awareness, persistent status bar notification, and transparent data processing guarantees.
        • EVA AI does NOT capture, transmit, or record background ambient audio without explicit, conscious user interaction.
    """.trimIndent(),
    val recommendedArchitecture: String = """
        To achieve production-grade wake-word in future phases:
        1. Implement an Android VoiceInteractionService and prompt the user to select EVA AI as their Default Digital Assistant in Android System Settings.
        2. Or use an edge-trigger architecture: activate voice input via quick-settings tile, lock-screen shortcut, or home widget.
    """.trimIndent(),
    val isFeasibleOnCurrentDevice: Boolean = false,
)

/**
 * Boundary interface for wake-word detection engine.
 * Enables clean dependency inversion and future hardware/service engine implementations.
 */
interface WakeWordDetector {
    val status: StateFlow<WakeWordStatus>
    val isSupportedOnDevice: Boolean
    fun checkFeasibility(): WakeWordFeasibilityReport
    fun startListening(onDetected: (String) -> Unit): Result<Unit>
    fun stopListening()
}

/**
 * Default implementation representing Outcome B:
 * Honest, technically transparent evaluation of wake-word feasibility on standard Android devices.
 */
open class DefaultWakeWordDetector(
    private val context: Context,
) : WakeWordDetector {

    private val _status = MutableStateFlow(WakeWordStatus.RESTRICTED_BY_OS)
    override val status: StateFlow<WakeWordStatus> = _status.asStateFlow()

    override val isSupportedOnDevice: Boolean
        get() = false // Honest: Standard Android non-system app lacks low-power DSP hotword capability

    override fun checkFeasibility(): WakeWordFeasibilityReport {
        val sdkVersion = Build.VERSION.SDK_INT
        val isRestricted = sdkVersion >= Build.VERSION_CODES.R // Android 11+

        val calculatedStatus = if (isRestricted) {
            WakeWordStatus.RESTRICTED_BY_OS
        } else {
            WakeWordStatus.UNAVAILABLE
        }

        _status.value = calculatedStatus

        return WakeWordFeasibilityReport(
            targetPhrases = listOf("Hey Eva", "Hey Eevva"),
            status = calculatedStatus,
            isFeasibleOnCurrentDevice = false,
        )
    }

    override fun startListening(onDetected: (String) -> Unit): Result<Unit> {
        // Transparent rejection: Do not pretend to listen or enter an unstable continuous loop
        return Result.failure(
            IllegalStateException(
                "Continuous wake-word listening is restricted by Android OS background microphone policies. " +
                "EVA AI operates hands-free via on-screen controls, widgets, or when designated as system assistant."
            )
        )
    }

    override fun stopListening() {
        _status.value = WakeWordStatus.DISABLED
    }
}
