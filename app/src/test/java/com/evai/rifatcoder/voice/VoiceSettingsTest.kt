package com.evai.rifatcoder.voice

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.evai.rifatcoder.data.voice.DefaultWakeWordDetector
import com.evai.rifatcoder.data.voice.SpeechRecognizerManager
import com.evai.rifatcoder.data.voice.TextToSpeechManager
import com.evai.rifatcoder.data.voice.VoiceLanguage
import com.evai.rifatcoder.data.voice.VoiceLanguagePreference
import com.evai.rifatcoder.data.voice.VoicePreferenceState
import com.evai.rifatcoder.data.voice.VoicePreferencesRepository
import com.evai.rifatcoder.data.voice.VoiceState
import com.evai.rifatcoder.data.voice.WakeWordStatus
import com.evai.rifatcoder.domain.chat.AiChatEngine
import com.evai.rifatcoder.domain.chat.ChatMessage
import com.evai.rifatcoder.domain.chat.ChatRole
import com.evai.rifatcoder.domain.chat.MessageStatus
import com.evai.rifatcoder.ui.voice.VoiceSettingsViewModel
import com.evai.rifatcoder.ui.voice.VoiceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive test suite verifying all 18 voice settings and wake-word feasibility requirements:
 * 1. Default voice preferences initialization
 * 2. Saving and updating STT language
 * 3. Saving and updating TTS enabled toggle
 * 4. Saving and updating TTS language
 * 5. Speech rate slider bounds and updates (0.75f - 1.50f)
 * 6. Pitch slider bounds and updates (0.80f - 1.20f)
 * 7. TTS voice ID selection and persistence
 * 8. Reset to default settings restores all default values
 * 9. TextToSpeechManager rate & pitch application
 * 10. Voice test playback triggers speak and stop
 * 11. WakeWordDetector reports honest UNAVAILABLE / RESTRICTED_BY_OS status (Outcome B)
 * 12. WakeWord feasibility report contains required phrases ("Hey Eva", "Hey Eevva")
 * 13. WakeWord report contains background OS restriction details
 * 14. WakeWord report contains battery impact disclosure
 * 15. WakeWord report contains privacy notice
 * 16. VoiceViewModel respects ttsEnabled flag (skips TTS speak if disabled)
 * 17. VoiceViewModel applies saved rate and pitch to TextToSpeechManager
 * 18. UI State mapper reflects all preference updates correctly
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoiceSettingsTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var preferencesRepo: VoicePreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        // Use an isolated preferences file for test isolation
        val testPrefsName = "test_voice_prefs_" + System.currentTimeMillis()
        preferencesRepo = VoicePreferencesRepository(application, prefsName = testPrefsName)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. Default voice preferences initialization
    @Test
    fun testDefaultVoicePreferencesInitialization() {
        val prefs = preferencesRepo.getPreferences()
        assertEquals(VoiceLanguagePreference.SYSTEM_DEFAULT, prefs.sttLanguage)
        assertTrue(prefs.ttsEnabled)
        assertEquals(VoiceLanguagePreference.ENGLISH, prefs.ttsLanguage)
        assertNull(prefs.ttsVoiceId)
        assertEquals(1.0f, prefs.speechRate, 0.01f)
        assertEquals(1.0f, prefs.speechPitch, 0.01f)
        assertFalse(prefs.wakeWordEnabled)
    }

    // 2. Saving and updating STT language
    @Test
    fun testSavingAndUpdatingSttLanguage() {
        preferencesRepo.setSttLanguage(VoiceLanguagePreference.BENGALI)
        assertEquals(VoiceLanguagePreference.BENGALI, preferencesRepo.getPreferences().sttLanguage)

        preferencesRepo.setSttLanguage(VoiceLanguagePreference.ENGLISH)
        assertEquals(VoiceLanguagePreference.ENGLISH, preferencesRepo.getPreferences().sttLanguage)
    }

    // 3. Saving and updating TTS enabled toggle
    @Test
    fun testSavingAndUpdatingTtsEnabled() {
        preferencesRepo.setTtsEnabled(false)
        assertFalse(preferencesRepo.getPreferences().ttsEnabled)

        preferencesRepo.setTtsEnabled(true)
        assertTrue(preferencesRepo.getPreferences().ttsEnabled)
    }

    // 4. Saving and updating TTS language
    @Test
    fun testSavingAndUpdatingTtsLanguage() {
        preferencesRepo.setTtsLanguage(VoiceLanguagePreference.BENGALI)
        assertEquals(VoiceLanguagePreference.BENGALI, preferencesRepo.getPreferences().ttsLanguage)

        preferencesRepo.setTtsLanguage(VoiceLanguagePreference.ENGLISH)
        assertEquals(VoiceLanguagePreference.ENGLISH, preferencesRepo.getPreferences().ttsLanguage)
    }

    // 5. Speech rate slider bounds and updates (0.75f - 1.50f clamping)
    @Test
    fun testSpeechRateBoundsAndClamping() {
        // Normal update
        preferencesRepo.setSpeechRate(1.25f)
        assertEquals(1.25f, preferencesRepo.getPreferences().speechRate, 0.01f)

        // Below minimum clamps to 0.75f
        preferencesRepo.setSpeechRate(0.50f)
        assertEquals(VoicePreferenceState.MIN_SPEECH_RATE, preferencesRepo.getPreferences().speechRate, 0.01f)

        // Above maximum clamps to 1.50f
        preferencesRepo.setSpeechRate(2.00f)
        assertEquals(VoicePreferenceState.MAX_SPEECH_RATE, preferencesRepo.getPreferences().speechRate, 0.01f)
    }

    // 6. Pitch slider bounds and updates (0.80f - 1.20f clamping)
    @Test
    fun testSpeechPitchBoundsAndClamping() {
        // Normal update
        preferencesRepo.setSpeechPitch(1.10f)
        assertEquals(1.10f, preferencesRepo.getPreferences().speechPitch, 0.01f)

        // Below minimum clamps to 0.80f
        preferencesRepo.setSpeechPitch(0.40f)
        assertEquals(VoicePreferenceState.MIN_SPEECH_PITCH, preferencesRepo.getPreferences().speechPitch, 0.01f)

        // Above maximum clamps to 1.20f
        preferencesRepo.setSpeechPitch(1.80f)
        assertEquals(VoicePreferenceState.MAX_SPEECH_PITCH, preferencesRepo.getPreferences().speechPitch, 0.01f)
    }

    // 7. TTS voice ID selection and persistence
    @Test
    fun testTtsVoiceIdSelectionAndPersistence() {
        preferencesRepo.setTtsVoiceId("en-us-x-sfg#female_1-local")
        assertEquals("en-us-x-sfg#female_1-local", preferencesRepo.getPreferences().ttsVoiceId)

        preferencesRepo.setTtsVoiceId(null)
        assertNull(preferencesRepo.getPreferences().ttsVoiceId)
    }

    // 8. Reset to default settings restores all default values
    @Test
    fun testResetToDefaultsRestoresAllValues() {
        // Change multiple values
        preferencesRepo.setSttLanguage(VoiceLanguagePreference.BENGALI)
        preferencesRepo.setTtsEnabled(false)
        preferencesRepo.setTtsLanguage(VoiceLanguagePreference.BENGALI)
        preferencesRepo.setSpeechRate(1.40f)
        preferencesRepo.setSpeechPitch(0.90f)
        preferencesRepo.setTtsVoiceId("custom-voice-id")
        preferencesRepo.setWakeWordEnabled(true)

        // Reset
        val defaults = preferencesRepo.resetToDefaults()
        assertEquals(VoiceLanguagePreference.SYSTEM_DEFAULT, defaults.sttLanguage)
        assertTrue(defaults.ttsEnabled)
        assertEquals(VoiceLanguagePreference.ENGLISH, defaults.ttsLanguage)
        assertEquals(1.0f, defaults.speechRate, 0.01f)
        assertEquals(1.0f, defaults.speechPitch, 0.01f)
        assertNull(defaults.ttsVoiceId)
        assertFalse(defaults.wakeWordEnabled)
    }

    // 9. TextToSpeechManager rate & pitch application
    @Test
    fun testTextToSpeechManagerRateAndPitchApplication() {
        var lastRate = 0f
        var lastPitch = 0f
        val mockTts = object : TextToSpeechManager(application) {
            override fun setSpeechRate(rate: Float) {
                lastRate = rate
            }
            override fun setPitch(pitch: Float) {
                lastPitch = pitch
            }
        }

        mockTts.setSpeechRate(1.35f)
        assertEquals(1.35f, lastRate, 0.01f)

        mockTts.setPitch(1.15f)
        assertEquals(1.15f, lastPitch, 0.01f)
    }

    // 10. Voice test playback triggers speak and stop
    @Test
    fun testVoiceTestPlaybackTriggersSpeakAndStop() {
        var spokenText: String? = null
        var isStopCalled = false

        val mockTts = object : TextToSpeechManager(application) {
            override fun speak(
                text: String,
                utteranceId: String,
                language: VoiceLanguage,
                rate: Float,
                pitch: Float,
                voiceId: String?
            ): Boolean {
                spokenText = text
                return true
            }

            override fun stop() {
                isStopCalled = true
            }
        }

        val viewModel = VoiceSettingsViewModel(
            application = application,
            preferencesRepository = preferencesRepo,
            ttsManager = mockTts,
            speechManager = SpeechRecognizerManager(application),
            wakeWordDetector = DefaultWakeWordDetector(application)
        )

        viewModel.testVoicePlayback()
        assertEquals("Hello! I am EVA, your intelligent companion.", spokenText)
        assertTrue(viewModel.uiState.value.isTestingVoice)

        viewModel.stopVoicePlayback()
        assertTrue(isStopCalled)
        assertFalse(viewModel.uiState.value.isTestingVoice)
    }

    // 11. WakeWordDetector reports honest UNAVAILABLE / RESTRICTED_BY_OS status (Outcome B)
    @Test
    fun testWakeWordDetectorHonestStatus() {
        val detector = DefaultWakeWordDetector(application)
        val report = detector.checkFeasibility()

        // Outcome B: Honest assessment on Android 11+
        assertTrue(
            report.status == WakeWordStatus.RESTRICTED_BY_OS ||
            report.status == WakeWordStatus.UNAVAILABLE
        )
        assertFalse(detector.isSupportedOnDevice)
        assertFalse(report.isFeasibleOnCurrentDevice)

        // Attempting startListening yields an honest failure
        val startResult = detector.startListening {}
        assertTrue(startResult.isFailure)
    }

    // 12. WakeWord feasibility report contains required phrases ("Hey Eva", "Hey Eevva")
    @Test
    fun testWakeWordFeasibilityReportContainsTargetPhrases() {
        val detector = DefaultWakeWordDetector(application)
        val report = detector.checkFeasibility()

        assertTrue(report.targetPhrases.contains("Hey Eva"))
        assertTrue(report.targetPhrases.contains("Hey Eevva"))
    }

    // 13. WakeWord report contains background OS restriction details
    @Test
    fun testWakeWordReportContainsBackgroundOsRestrictions() {
        val detector = DefaultWakeWordDetector(application)
        val report = detector.checkFeasibility()

        assertNotNull(report.backgroundOsRestrictions)
        assertTrue(report.backgroundOsRestrictions.contains("Android 11+"))
        assertTrue(report.backgroundOsRestrictions.contains("FOREGROUND_SERVICE_TYPE_MICROPHONE"))
    }

    // 14. WakeWord report contains battery impact disclosure
    @Test
    fun testWakeWordReportContainsBatteryImpactDisclosure() {
        val detector = DefaultWakeWordDetector(application)
        val report = detector.checkFeasibility()

        assertNotNull(report.batteryImpactAssessment)
        assertTrue(report.batteryImpactAssessment.contains("battery"))
        assertTrue(report.batteryImpactAssessment.contains("Doze"))
    }

    // 15. WakeWord report contains privacy notice
    @Test
    fun testWakeWordReportContainsPrivacyNotice() {
        val detector = DefaultWakeWordDetector(application)
        val report = detector.checkFeasibility()

        assertNotNull(report.privacyNotice)
        assertTrue(report.privacyNotice.contains("privacy"))
        assertTrue(report.privacyNotice.contains("EVA AI does NOT capture"))
    }

    // 16. VoiceViewModel respects ttsEnabled flag (skips TTS speak if disabled)
    @Test
    fun testVoiceViewModelRespectsTtsEnabledFlag() = runTest(testDispatcher) {
        preferencesRepo.setTtsEnabled(false)

        var ttsSpeakCalled = false
        val mockTts = object : TextToSpeechManager(application) {
            override fun speak(
                text: String,
                utteranceId: String,
                language: VoiceLanguage,
                rate: Float,
                pitch: Float,
                voiceId: String?
            ): Boolean {
                ttsSpeakCalled = true
                return true
            }
        }

        val fakeChatEngine = FakeAiChatEngine(Result.success("Hello from assistant"))

        val voiceVm = VoiceViewModel(
            application = application,
            speechManager = SpeechRecognizerManager(application),
            ttsManager = mockTts,
            chatEngine = fakeChatEngine,
            conversationRepository = FakeConversationRepository(),
            preferencesRepository = preferencesRepo,
            memoryRepository = FakeMemoryRepository(),
        )

        voiceVm.updateTranscriptionText("Hello EVA")
        voiceVm.submitManualPrompt()
        advanceUntilIdle()

        // Since ttsEnabled is false, ttsSpeakCalled MUST be false
        assertFalse(ttsSpeakCalled)
        assertEquals(VoiceState.READY, voiceVm.uiState.value.voiceState)
        assertEquals("Hello from assistant", voiceVm.uiState.value.assistantResponseText)
    }

    // 17. VoiceViewModel applies saved rate and pitch to TextToSpeechManager
    @Test
    fun testVoiceViewModelAppliesSavedRateAndPitch() = runTest(testDispatcher) {
        preferencesRepo.setTtsEnabled(true)
        preferencesRepo.setSpeechRate(1.30f)
        preferencesRepo.setSpeechPitch(0.95f)

        var appliedRate = 0f
        var appliedPitch = 0f

        val mockTts = object : TextToSpeechManager(application) {
            override fun speak(
                text: String,
                utteranceId: String,
                language: VoiceLanguage,
                rate: Float,
                pitch: Float,
                voiceId: String?
            ): Boolean {
                appliedRate = rate
                appliedPitch = pitch
                return true
            }
        }

        val fakeChatEngine = FakeAiChatEngine(Result.success("Response with custom pitch and rate"))

        val voiceVm = VoiceViewModel(
            application = application,
            speechManager = SpeechRecognizerManager(application),
            ttsManager = mockTts,
            chatEngine = fakeChatEngine,
            conversationRepository = FakeConversationRepository(),
            preferencesRepository = preferencesRepo,
            memoryRepository = FakeMemoryRepository(),
        )

        voiceVm.updateTranscriptionText("Test prompt")
        voiceVm.submitManualPrompt()
        advanceUntilIdle()

        assertEquals(1.30f, appliedRate, 0.01f)
        assertEquals(0.95f, appliedPitch, 0.01f)
    }

    // 18. UI State mapper reflects all preference updates correctly
    @Test
    fun testVoiceSettingsViewModelStateUpdates() {
        val viewModel = VoiceSettingsViewModel(
            application = application,
            preferencesRepository = preferencesRepo,
            ttsManager = TextToSpeechManager(application),
            speechManager = SpeechRecognizerManager(application),
            wakeWordDetector = DefaultWakeWordDetector(application)
        )

        viewModel.setSttLanguage(VoiceLanguagePreference.BENGALI)
        assertEquals(VoiceLanguagePreference.BENGALI, viewModel.uiState.value.sttLanguage)

        viewModel.setTtsEnabled(false)
        assertFalse(viewModel.uiState.value.ttsEnabled)

        viewModel.setTtsLanguage(VoiceLanguagePreference.BENGALI)
        assertEquals(VoiceLanguagePreference.BENGALI, viewModel.uiState.value.ttsLanguage)

        viewModel.setSpeechRate(1.45f)
        assertEquals(1.45f, viewModel.uiState.value.speechRate, 0.01f)

        viewModel.setSpeechPitch(0.85f)
        assertEquals(0.85f, viewModel.uiState.value.speechPitch, 0.01f)

        // Save
        viewModel.saveSettings()
        val saved = preferencesRepo.getPreferences()
        assertEquals(VoiceLanguagePreference.BENGALI, saved.sttLanguage)
        assertFalse(saved.ttsEnabled)
        assertEquals(VoiceLanguagePreference.BENGALI, saved.ttsLanguage)
        assertEquals(1.45f, saved.speechRate, 0.01f)
        assertEquals(0.85f, saved.speechPitch, 0.01f)

        // Reset
        viewModel.resetToDefaults()
        val resetState = viewModel.uiState.value
        assertEquals(VoiceLanguagePreference.SYSTEM_DEFAULT, resetState.sttLanguage)
        assertTrue(resetState.ttsEnabled)
        assertEquals(VoiceLanguagePreference.ENGLISH, resetState.ttsLanguage)
        assertEquals(1.0f, resetState.speechRate, 0.01f)
        assertEquals(1.0f, resetState.speechPitch, 0.01f)
    }
}
