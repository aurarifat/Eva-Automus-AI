package com.evai.rifatcoder.domain.glow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Representation of EVA AI real-time voice and automation states for edge glow rendering.
 */
enum class EdgeGlowState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    WAITING_FOR_CONFIRMATION,
    EXECUTING,
    SUCCESS,
    FAILED,
    ERROR,
    CANCELLED;

    val displayName: String
        get() = when (this) {
            IDLE -> "Idle"
            LISTENING -> "Listening"
            THINKING -> "Thinking / Planning"
            SPEAKING -> "Speaking"
            WAITING_FOR_CONFIRMATION -> "Waiting for Confirmation"
            EXECUTING -> "Executing Task"
            SUCCESS -> "Success"
            FAILED, ERROR -> "Failed"
            CANCELLED -> "Cancelled"
        }
}

/**
 * Visual intensity settings controlling line thickness, glow spread, and alpha.
 */
enum class EdgeGlowIntensity(
    val id: String,
    val displayName: String,
    val coreStrokeWidth: Dp,
    val outerGlowWidth: Dp,
    val maxOpacity: Float,
) {
    LOW(
        id = "low",
        displayName = "Low",
        coreStrokeWidth = 2.0.dp,
        outerGlowWidth = 6.0.dp,
        maxOpacity = 0.45f,
    ),
    MEDIUM(
        id = "medium",
        displayName = "Medium",
        coreStrokeWidth = 3.5.dp,
        outerGlowWidth = 10.0.dp,
        maxOpacity = 0.75f,
    ),
    HIGH(
        id = "high",
        displayName = "High",
        coreStrokeWidth = 5.0.dp,
        outerGlowWidth = 15.0.dp,
        maxOpacity = 1.0f,
    );

    companion object {
        fun fromId(id: String?): EdgeGlowIntensity =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: MEDIUM
    }
}

/**
 * User selectable color theme for the edge glow.
 */
enum class EdgeGlowColorOption(
    val id: String,
    val displayName: String,
) {
    AUTO(id = "auto", displayName = "Auto (Purple & Cyan)"),
    PURPLE(id = "purple", displayName = "Royal Purple"),
    CYAN(id = "cyan", displayName = "Electric Cyan");

    fun getColors(state: EdgeGlowState): List<Color> {
        val primaryPurple = Color(0xFF8A7FFF)
        val secondaryCyan = Color(0xFF00E5FF)
        val violetAccent = Color(0xFF6C60F6)
        val softCyan = Color(0xFF80D8FF)
        val softPurple = Color(0xFFB388FF)
        val emeraldSuccess = Color(0xFF10B981)
        val mintSuccess = Color(0xFF34D399)
        val amberAlert = Color(0xFFF59E0B)
        val errorRed = Color(0xFFEF4444)
        val crimsonDark = Color(0xFFDC2626)
        val slateMuted = Color(0xFF64748B)

        return when (state) {
            EdgeGlowState.WAITING_FOR_CONFIRMATION -> listOf(amberAlert, primaryPurple, amberAlert)
            EdgeGlowState.SUCCESS -> listOf(emeraldSuccess, secondaryCyan, mintSuccess, emeraldSuccess)
            EdgeGlowState.FAILED, EdgeGlowState.ERROR -> listOf(errorRed, amberAlert, crimsonDark)
            EdgeGlowState.CANCELLED -> listOf(violetAccent.copy(alpha = 0.6f), slateMuted, primaryPurple.copy(alpha = 0.4f))
            EdgeGlowState.EXECUTING -> when (this) {
                PURPLE -> listOf(primaryPurple, violetAccent, softPurple)
                CYAN -> listOf(secondaryCyan, softCyan, Color(0xFF00B8D4))
                AUTO -> listOf(secondaryCyan, primaryPurple, secondaryCyan, violetAccent)
            }
            EdgeGlowState.LISTENING -> when (this) {
                PURPLE -> listOf(primaryPurple, softPurple, violetAccent)
                CYAN -> listOf(secondaryCyan, softCyan, Color(0xFF00B8D4))
                AUTO -> listOf(secondaryCyan, primaryPurple, secondaryCyan)
            }
            EdgeGlowState.THINKING -> when (this) {
                PURPLE -> listOf(violetAccent, primaryPurple, violetAccent)
                CYAN -> listOf(Color(0xFF00B8D4), secondaryCyan, Color(0xFF00B8D4))
                AUTO -> listOf(primaryPurple, violetAccent, primaryPurple, secondaryCyan)
            }
            EdgeGlowState.SPEAKING -> when (this) {
                PURPLE -> listOf(primaryPurple, violetAccent, primaryPurple)
                CYAN -> listOf(secondaryCyan, softCyan, secondaryCyan)
                AUTO -> listOf(secondaryCyan, primaryPurple, softCyan, primaryPurple)
            }
            EdgeGlowState.IDLE -> when (this) {
                PURPLE -> listOf(primaryPurple.copy(alpha = 0.3f), violetAccent.copy(alpha = 0.2f))
                CYAN -> listOf(secondaryCyan.copy(alpha = 0.3f), softCyan.copy(alpha = 0.2f))
                AUTO -> listOf(primaryPurple.copy(alpha = 0.25f), secondaryCyan.copy(alpha = 0.25f))
            }
        }
    }

    companion object {
        fun fromId(id: String?): EdgeGlowColorOption =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: AUTO
    }
}

/**
 * Immutable user preferences data class for Edge Glow.
 */
data class EdgeGlowPreferences(
    val isEnabled: Boolean = true,
    val intensity: EdgeGlowIntensity = EdgeGlowIntensity.MEDIUM,
    val colorOption: EdgeGlowColorOption = EdgeGlowColorOption.AUTO,
    val showDuringListening: Boolean = true,
    val showDuringSpeaking: Boolean = true,
    val showDuringThinking: Boolean = true,
    val showDuringExecuting: Boolean = true,
    val isSystemOverlayEnabled: Boolean = false,
) {
    /**
     * Checks if the edge glow should be actively visible for a specific voice/automation state.
     */
    fun shouldGlowForState(state: EdgeGlowState): Boolean {
        if (!isEnabled) return false
        return when (state) {
            EdgeGlowState.IDLE -> false
            EdgeGlowState.LISTENING -> showDuringListening
            EdgeGlowState.THINKING -> showDuringThinking
            EdgeGlowState.SPEAKING -> showDuringSpeaking
            EdgeGlowState.WAITING_FOR_CONFIRMATION -> true
            EdgeGlowState.EXECUTING -> showDuringExecuting
            EdgeGlowState.SUCCESS -> true
            EdgeGlowState.FAILED, EdgeGlowState.ERROR -> true
            EdgeGlowState.CANCELLED -> true
        }
    }
}
