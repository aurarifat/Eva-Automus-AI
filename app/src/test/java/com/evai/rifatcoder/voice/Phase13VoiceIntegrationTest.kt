package com.evai.rifatcoder.voice

import com.evai.rifatcoder.data.glow.EdgeGlowStateHolder
import com.evai.rifatcoder.data.voice.CentralVoiceState
import com.evai.rifatcoder.data.voice.CentralVoiceStateManager
import com.evai.rifatcoder.data.voice.DefaultWakeWordDetector
import com.evai.rifatcoder.data.voice.VoiceLanguage
import com.evai.rifatcoder.data.voice.VoiceState
import com.evai.rifatcoder.data.voice.WakeWordStatus
import com.evai.rifatcoder.domain.glow.EdgeGlowState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase13VoiceIntegrationTest {

    private val centralManager = CentralVoiceStateManager

    @Before
    fun setUp() {
        centralManager.resetToIdle()
    }

    @After
    fun tearDown() {
        centralManager.stopAll()
    }

    @Test
    fun testAuthoritativeStateTransitions_idleToSpeakingAndBack() {
        // Initial state is IDLE
        assertEquals(CentralVoiceState.IDLE, centralManager.currentState)
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)
        assertEquals(0f, centralManager.currentSoundLevel, 0.001f)

        // 1. User taps mic -> STARTING (NOT LISTENING yet)
        val accepted = centralManager.requestStartListening(hasPermission = true)
        assertTrue(accepted)
        assertEquals(CentralVoiceState.STARTING, centralManager.currentState)
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)

        // 2. Recognizer is ready -> LISTENING
        centralManager.notifyReadyForSpeech()
        assertEquals(CentralVoiceState.LISTENING, centralManager.currentState)
        assertEquals(EdgeGlowState.LISTENING, EdgeGlowStateHolder.glowState.value)

        // 3. User speaks with amplitude -> soundLevel updates
        centralManager.notifyRmsChanged(4.0f)
        val level = centralManager.currentSoundLevel
        assertTrue(level > 0f)
        assertEquals(level, EdgeGlowStateHolder.soundLevel.value, 0.001f)

        // 4. Speech finishes -> PROCESSING
        centralManager.notifyEndOfSpeech()
        assertEquals(CentralVoiceState.PROCESSING, centralManager.currentState)
        assertEquals(EdgeGlowState.THINKING, EdgeGlowStateHolder.glowState.value)
        assertEquals(0f, centralManager.currentSoundLevel, 0.001f)

        // 5. Final transcript confirmed
        centralManager.notifySpeechFinalResult("What is quantum computing?")
        assertEquals(CentralVoiceState.PROCESSING, centralManager.currentState)
        assertEquals("What is quantum computing?", centralManager.transcribedText.value)

        // 6. TTS audio playback starts -> SPEAKING
        centralManager.notifyTtsStarted("utterance-quantum-1")
        assertEquals(CentralVoiceState.SPEAKING, centralManager.currentState)
        assertEquals(EdgeGlowState.SPEAKING, EdgeGlowStateHolder.glowState.value)

        // 7. TTS audio playback finishes -> resets cleanly to IDLE
        centralManager.notifyTtsFinished("utterance-quantum-1")
        assertEquals(CentralVoiceState.IDLE, centralManager.currentState)
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)
        assertEquals(0f, centralManager.currentSoundLevel, 0.001f)
    }

    @Test
    fun testContradictoryStatePrevention_cannotStartListeningWhileAlreadyListeningOrStarting() {
        // Start listening session
        centralManager.requestStartListening(hasPermission = true)
        assertEquals(CentralVoiceState.STARTING, centralManager.currentState)

        // Attempt second duplicate session while STARTING -> rejected
        val duplicate1 = centralManager.requestStartListening(hasPermission = true)
        assertFalse("Duplicate mic session while STARTING must be rejected", duplicate1)

        // Transition to LISTENING
        centralManager.notifyReadyForSpeech()
        assertEquals(CentralVoiceState.LISTENING, centralManager.currentState)

        // Attempt second duplicate session while LISTENING -> rejected
        val duplicate2 = centralManager.requestStartListening(hasPermission = true)
        assertFalse("Duplicate mic session while LISTENING must be rejected", duplicate2)
        assertEquals(CentralVoiceState.LISTENING, centralManager.currentState)
    }

    @Test
    fun testRapidTapDebounce() {
        val t0 = 100_000L
        assertTrue("First tap accepted", centralManager.canAcceptTap(t0))

        // Rapid tap 100ms later (within 350ms window) -> rejected
        assertFalse("Rapid tap within 350ms window must be debounced", centralManager.canAcceptTap(t0 + 100L))
        assertFalse("Rapid tap within 350ms window must be debounced", centralManager.canAcceptTap(t0 + 250L))

        // Tap 400ms later -> accepted
        assertTrue("Tap after debounce window must be accepted", centralManager.canAcceptTap(t0 + 400L))
    }

    @Test
    fun testDoNotReportListeningUntilGenuinelyActive() {
        centralManager.requestStartListening(hasPermission = true)
        // Must NOT be LISTENING yet!
        assertEquals("State must be STARTING before recognizer onReady callback",
            CentralVoiceState.STARTING, centralManager.currentState)
        assertFalse("Visual glow must not show LISTENING while preparing",
            EdgeGlowStateHolder.glowState.value == EdgeGlowState.LISTENING)

        centralManager.notifyReadyForSpeech()
        assertEquals(CentralVoiceState.LISTENING, centralManager.currentState)
        assertEquals(EdgeGlowState.LISTENING, EdgeGlowStateHolder.glowState.value)
    }

    @Test
    fun testDoNotReportSpeakingUntilTtsAudioPlaybackStarts() {
        centralManager.notifySpeechFinalResult("Tell me a story")
        assertEquals(CentralVoiceState.PROCESSING, centralManager.currentState)

        // Setting assistant response does not transition to SPEAKING
        centralManager.setAssistantResponse("Once upon a time...")
        assertEquals("State must remain PROCESSING until onSpeechStarted callback",
            CentralVoiceState.PROCESSING, centralManager.currentState)

        // Only when TTS engine begins streaming audio does state become SPEAKING
        centralManager.notifyTtsStarted("story-utt-1")
        assertEquals(CentralVoiceState.SPEAKING, centralManager.currentState)
        assertEquals(EdgeGlowState.SPEAKING, EdgeGlowStateHolder.glowState.value)
    }

    @Test
    fun testTimeoutAndNoMatchResetCleanlyToIdleWithoutStuckState() {
        centralManager.requestStartListening(hasPermission = true)
        centralManager.notifyReadyForSpeech()
        assertEquals(CentralVoiceState.LISTENING, centralManager.currentState)

        // Timeout occurs (e.g. user opens mic but says nothing)
        centralManager.notifyError("No speech detected within time limit.", isTimeout = true)

        assertEquals("Timeout must reset cleanly to IDLE", CentralVoiceState.IDLE, centralManager.currentState)
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)
        assertNull("Timeout must not leave an active error banner", centralManager.errorMessage.value)
    }

    @Test
    fun testEmptySpeechResultResetsCleanlyToIdle() {
        centralManager.requestStartListening(hasPermission = true)
        centralManager.notifyReadyForSpeech()
        centralManager.notifyEndOfSpeech()

        // Empty transcript received
        centralManager.notifySpeechFinalResult("   ")

        assertEquals(CentralVoiceState.IDLE, centralManager.currentState)
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)
        assertEquals(0f, centralManager.currentSoundLevel, 0.001f)
    }

    @Test
    fun testExecutingTaskTransitions() {
        centralManager.notifyExecutingTask(true)
        assertEquals(CentralVoiceState.EXECUTING_TASK, centralManager.currentState)
        assertEquals(EdgeGlowState.EXECUTING, EdgeGlowStateHolder.glowState.value)

        centralManager.notifyExecutingTask(false)
        assertEquals(CentralVoiceState.IDLE, centralManager.currentState)
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)
    }

    @Test
    fun testStopAllCleanlyResetsEverything() {
        centralManager.requestStartListening(hasPermission = true)
        centralManager.notifyReadyForSpeech()
        centralManager.notifyRmsChanged(8f)

        centralManager.stopAll()

        assertEquals(CentralVoiceState.IDLE, centralManager.currentState)
        assertEquals(0f, centralManager.currentSoundLevel, 0.001f)
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)
        assertNull(centralManager.errorMessage.value)
    }

    @Test
    fun testTtsErrorResetsToIdle() {
        centralManager.notifyTtsStarted("fail-utt-1")
        assertEquals(CentralVoiceState.SPEAKING, centralManager.currentState)

        centralManager.notifyTtsError("fail-utt-1", -1)
        assertEquals(CentralVoiceState.IDLE, centralManager.currentState)
        assertEquals(EdgeGlowState.IDLE, EdgeGlowStateHolder.glowState.value)
    }

    @Test
    fun testWakeWordTruthfulReporting() {
        val context = RuntimeEnvironment.getApplication()
        val detector = DefaultWakeWordDetector(context)

        // Wake word must be truthfully reported as not supported/restricted
        assertFalse("Third party background hotword must not claim hardware support", detector.isSupportedOnDevice)

        val report = detector.checkFeasibility()
        assertFalse(report.isFeasibleOnCurrentDevice)
        assertTrue(
            report.status == WakeWordStatus.RESTRICTED_BY_OS ||
                    report.status == WakeWordStatus.UNAVAILABLE ||
                    report.status == WakeWordStatus.NOT_CONFIGURED
        )
    }

    @Test
    fun testLanguageSelection() {
        assertEquals("en", VoiceLanguage.ENGLISH.code)
        assertEquals("en-US", VoiceLanguage.ENGLISH.localeTag)
        assertEquals("bn", VoiceLanguage.BENGALI.code)
        assertEquals("bn-BD", VoiceLanguage.BENGALI.localeTag)
    }

    @Test
    fun testLegacyVoiceStateMapping() {
        assertEquals(CentralVoiceState.IDLE, VoiceState.READY.toCentralVoiceState())
        assertEquals(CentralVoiceState.STARTING, VoiceState.REQUESTING_PERMISSION.toCentralVoiceState())
        assertEquals(CentralVoiceState.LISTENING, VoiceState.LISTENING.toCentralVoiceState())
        assertEquals(CentralVoiceState.PROCESSING, VoiceState.THINKING.toCentralVoiceState())
        assertEquals(CentralVoiceState.PROCESSING, VoiceState.RECOGNIZING.toCentralVoiceState())
        assertEquals(CentralVoiceState.SPEAKING, VoiceState.SPEAKING.toCentralVoiceState())
        assertEquals(CentralVoiceState.ERROR, VoiceState.ERROR.toCentralVoiceState())
    }
}
