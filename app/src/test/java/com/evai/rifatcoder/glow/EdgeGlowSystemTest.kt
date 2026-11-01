package com.evai.rifatcoder.glow

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.evai.rifatcoder.data.glow.EdgeGlowOverlayService
import com.evai.rifatcoder.data.glow.EdgeGlowPreferencesRepository
import com.evai.rifatcoder.data.glow.EdgeGlowStateHolder
import com.evai.rifatcoder.data.voice.VoiceState
import com.evai.rifatcoder.domain.glow.EdgeGlowColorOption
import com.evai.rifatcoder.domain.glow.EdgeGlowIntensity
import com.evai.rifatcoder.domain.glow.EdgeGlowPreferences
import com.evai.rifatcoder.domain.glow.EdgeGlowState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EdgeGlowSystemTest {

    private lateinit var application: Application
    private lateinit var repository: EdgeGlowPreferencesRepository

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        repository = EdgeGlowPreferencesRepository(
            context = application,
            prefsName = "test_edge_glow_prefs_" + System.currentTimeMillis()
        )
        EdgeGlowStateHolder.resetToIdle()
    }

    @After
    fun tearDown() {
        EdgeGlowStateHolder.resetToIdle()
    }

    // 1. Edge Glow Preferences defaults
    @Test
    fun testEdgeGlowPreferencesDefaults() {
        val defaults = repository.getPreferences()
        assertTrue(defaults.isEnabled)
        assertEquals(EdgeGlowIntensity.MEDIUM, defaults.intensity)
        assertEquals(EdgeGlowColorOption.AUTO, defaults.colorOption)
        assertTrue(defaults.showDuringListening)
        assertTrue(defaults.showDuringSpeaking)
        assertTrue(defaults.showDuringThinking)
        assertTrue(defaults.showDuringExecuting)
        assertFalse(defaults.isSystemOverlayEnabled)
    }

    // 2. Preferences modification and persistence
    @Test
    fun testPreferencesModificationAndPersistence() {
        repository.setEnabled(false)
        assertFalse(repository.getPreferences().isEnabled)

        repository.setIntensity(EdgeGlowIntensity.HIGH)
        assertEquals(EdgeGlowIntensity.HIGH, repository.getPreferences().intensity)

        repository.setColorOption(EdgeGlowColorOption.PURPLE)
        assertEquals(EdgeGlowColorOption.PURPLE, repository.getPreferences().colorOption)

        repository.setShowDuringListening(false)
        assertFalse(repository.getPreferences().showDuringListening)

        repository.setShowDuringSpeaking(false)
        assertFalse(repository.getPreferences().showDuringSpeaking)

        repository.setShowDuringThinking(false)
        assertFalse(repository.getPreferences().showDuringThinking)

        repository.setShowDuringExecuting(false)
        assertFalse(repository.getPreferences().showDuringExecuting)

        repository.setSystemOverlayEnabled(true)
        assertTrue(repository.getPreferences().isSystemOverlayEnabled)

        // Reset to defaults
        val reset = repository.resetToDefaults()
        assertTrue(reset.isEnabled)
        assertEquals(EdgeGlowIntensity.MEDIUM, reset.intensity)
        assertEquals(EdgeGlowColorOption.AUTO, reset.colorOption)
        assertTrue(reset.showDuringListening)
        assertTrue(reset.showDuringSpeaking)
    }

    // 3. shouldGlowForState evaluation with master switch and state triggers
    @Test
    fun testShouldGlowForStateEvaluation() {
        var prefs = EdgeGlowPreferences(isEnabled = true)

        // Idle state should not glow
        assertFalse(prefs.shouldGlowForState(EdgeGlowState.IDLE))

        // Active voice states should glow
        assertTrue(prefs.shouldGlowForState(EdgeGlowState.LISTENING))
        assertTrue(prefs.shouldGlowForState(EdgeGlowState.THINKING))
        assertTrue(prefs.shouldGlowForState(EdgeGlowState.SPEAKING))
        assertTrue(prefs.shouldGlowForState(EdgeGlowState.EXECUTING))
        assertTrue(prefs.shouldGlowForState(EdgeGlowState.ERROR))

        // When master is disabled, nothing glows
        prefs = prefs.copy(isEnabled = false)
        assertFalse(prefs.shouldGlowForState(EdgeGlowState.LISTENING))
        assertFalse(prefs.shouldGlowForState(EdgeGlowState.THINKING))
        assertFalse(prefs.shouldGlowForState(EdgeGlowState.SPEAKING))
        assertFalse(prefs.shouldGlowForState(EdgeGlowState.EXECUTING))
        assertFalse(prefs.shouldGlowForState(EdgeGlowState.ERROR))

        // Selective trigger toggles
        val customPrefs = EdgeGlowPreferences(
            isEnabled = true,
            showDuringListening = false,
            showDuringSpeaking = true,
            showDuringThinking = false,
        )
        assertFalse(customPrefs.shouldGlowForState(EdgeGlowState.LISTENING))
        assertTrue(customPrefs.shouldGlowForState(EdgeGlowState.SPEAKING))
        assertFalse(customPrefs.shouldGlowForState(EdgeGlowState.THINKING))
    }

    // 4. VoiceState mapping to EdgeGlowState in EdgeGlowStateHolder
    @Test
    fun testVoiceStateMappingInStateHolder() {
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)

        // Ready / Permission -> IDLE
        EdgeGlowStateHolder.updateVoiceState(VoiceState.READY)
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)

        EdgeGlowStateHolder.updateVoiceState(VoiceState.REQUESTING_PERMISSION)
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)

        // LISTENING
        EdgeGlowStateHolder.updateVoiceState(VoiceState.LISTENING, currentSoundLevel = 0.6f)
        assertEquals(EdgeGlowState.LISTENING, EdgeGlowStateHolder.glowState.value)
        assertEquals(0.6f, EdgeGlowStateHolder.soundLevel.value, 0.01f)

        // RECOGNIZING / THINKING
        EdgeGlowStateHolder.updateVoiceState(VoiceState.RECOGNIZING)
        assertEquals(EdgeGlowState.THINKING, EdgeGlowStateHolder.glowState.value)

        EdgeGlowStateHolder.updateVoiceState(VoiceState.THINKING)
        assertEquals(EdgeGlowState.THINKING, EdgeGlowStateHolder.glowState.value)

        // SPEAKING with audio amplitude
        EdgeGlowStateHolder.updateVoiceState(VoiceState.SPEAKING, currentSoundLevel = 0.85f)
        assertEquals(EdgeGlowState.SPEAKING, EdgeGlowStateHolder.glowState.value)
        assertEquals(0.85f, EdgeGlowStateHolder.soundLevel.value, 0.01f)

        // ERROR
        EdgeGlowStateHolder.updateVoiceState(VoiceState.ERROR)
        assertEquals(EdgeGlowState.ERROR, EdgeGlowStateHolder.glowState.value)

        // Return to READY -> IDLE
        EdgeGlowStateHolder.updateVoiceState(VoiceState.READY)
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)
        assertEquals(0f, EdgeGlowStateHolder.soundLevel.value, 0.01f)
    }

    // 5. Sound level clamping to [0.0f, 1.0f]
    @Test
    fun testSoundLevelClamping() {
        // Values > 1.0f must clamp to 1.0f
        EdgeGlowStateHolder.updateVoiceState(VoiceState.SPEAKING, currentSoundLevel = 2.5f)
        assertEquals(1.0f, EdgeGlowStateHolder.soundLevel.value, 0.01f)

        // Values < 0.0f must clamp to 0.0f
        EdgeGlowStateHolder.updateVoiceState(VoiceState.LISTENING, currentSoundLevel = -0.5f)
        assertEquals(0.0f, EdgeGlowStateHolder.soundLevel.value, 0.01f)
    }

    // 6. Automation executing state mapping
    @Test
    fun testAutomationExecutingStateMapping() {
        EdgeGlowStateHolder.setAutomationExecuting(true)
        assertEquals(EdgeGlowState.EXECUTING, EdgeGlowStateHolder.glowState.value)

        // Ending execution restores IDLE
        EdgeGlowStateHolder.setAutomationExecuting(false)
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)

        // Trigger error alert
        EdgeGlowStateHolder.triggerErrorAlert()
        assertEquals(EdgeGlowState.ERROR, EdgeGlowStateHolder.glowState.value)
    }

    // 7. State simulation and simulator clearance
    @Test
    fun testStateSimulationAndClearing() {
        EdgeGlowStateHolder.simulateState(EdgeGlowState.THINKING, simulatedSoundLevel = 0.5f)
        assertEquals(EdgeGlowState.THINKING, EdgeGlowStateHolder.glowState.value)

        // While simulation is active, voice engine updates should not override
        EdgeGlowStateHolder.updateVoiceState(VoiceState.LISTENING, 0.9f)
        assertEquals(EdgeGlowState.THINKING, EdgeGlowStateHolder.glowState.value)

        // Stopping simulation restores true state
        EdgeGlowStateHolder.stopSimulation()
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)
    }

    // 8. Color palette generation
    @Test
    fun testColorPaletteGeneration() {
        val autoColors = EdgeGlowColorOption.AUTO.getColors(EdgeGlowState.LISTENING)
        assertTrue(autoColors.isNotEmpty())

        val purpleColors = EdgeGlowColorOption.PURPLE.getColors(EdgeGlowState.SPEAKING)
        assertTrue(purpleColors.isNotEmpty())

        val cyanColors = EdgeGlowColorOption.CYAN.getColors(EdgeGlowState.EXECUTING)
        assertTrue(cyanColors.isNotEmpty())

        val errorColors = EdgeGlowColorOption.AUTO.getColors(EdgeGlowState.ERROR)
        assertTrue(errorColors.isNotEmpty())
    }

    // 9. Overlay permission check function
    @Test
    fun testOverlayPermissionCheckFunction() {
        // In Robolectric test context without granted overlay permission, returns false safely without crashing
        val canDraw = EdgeGlowOverlayService.canDrawOverlays(application)
        assertNotNull(canDraw)
    }
}
