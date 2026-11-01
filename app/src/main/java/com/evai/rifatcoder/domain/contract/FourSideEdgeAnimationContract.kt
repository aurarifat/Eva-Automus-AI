package com.evai.rifatcoder.domain.contract

/**
 * Architectural contract for future Four-Side Edge & Corner Animation.
 *
 * CRITICAL REQUIREMENTS (Future Phase):
 * 1. Must display an animated border around the FOUR EDGES AND CORNERS of the screen.
 * 2. It is NOT a floating bubble, NOT a circular chat head, and NOT a floating draggable button.
 * 3. Thin luminous border with purple (#8A7FFF) and cyan (#00E5FF) glow effects.
 * 4. Must reflect EVA's genuine runtime state:
 *    - IDLE: Subtle ambient border glow
 *    - LISTENING: Responsive audio-reactive pulse
 *    - THINKING: Continuous traveling edge stream
 *    - SPEAKING: Modulated rhythmic edge glow
 *    - EXECUTING: Distinct progress animation
 *    - ERROR: Identifiable warning border
 * 5. Native Android overlay window with transparent touch-through regions.
 * 6. Properly managed lifecycle via Foreground Service with SYSTEM_ALERT_WINDOW permission.
 */
interface FourSideEdgeAnimationContract {
    enum class EdgeVisualState {
        Idle,
        Listening,
        Thinking,
        Speaking,
        Executing,
        Error,
        Hidden,
    }

    enum class OverlayPermissionStatus {
        NotRequested,
        Denied,
        Granted,
    }

    val visualState: EdgeVisualState
    val overlayPermission: OverlayPermissionStatus

    fun updateVisualState(newState: EdgeVisualState)
}
