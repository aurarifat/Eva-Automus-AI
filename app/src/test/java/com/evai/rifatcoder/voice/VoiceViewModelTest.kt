package com.evai.rifatcoder.voice

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.evai.rifatcoder.data.database.entity.ConversationEntity
import com.evai.rifatcoder.data.repository.ConversationRepository
import com.evai.rifatcoder.data.voice.SpeechRecognitionCallback
import com.evai.rifatcoder.data.voice.SpeechRecognizerManager
import com.evai.rifatcoder.data.voice.TextToSpeechManager
import com.evai.rifatcoder.data.voice.TtsCallback
import com.evai.rifatcoder.data.voice.VoiceLanguage
import com.evai.rifatcoder.data.voice.VoiceState
import com.evai.rifatcoder.domain.chat.AiChatEngine
import com.evai.rifatcoder.domain.chat.ChatMessage
import com.evai.rifatcoder.domain.chat.ChatRole
import com.evai.rifatcoder.ui.voice.VoiceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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

class FakeAiChatEngine(
    var responseResult: Result<String> = Result.success("Hello from EVA AI!"),
) : AiChatEngine {
    override val isConfigured: Boolean = true
    override val activeModelName: String = "Test Model"
    override val activeProviderType = com.evai.rifatcoder.data.security.AiProviderType.GEMINI

    var lastPrompt: String? = null
    var promptCallCount = 0

    override suspend fun sendPrompt(
        conversationId: String,
        history: List<ChatMessage>,
        prompt: String,
    ): Result<String> {
        lastPrompt = prompt
        promptCallCount++
        return responseResult
    }
}

class FakeSpeechRecognizerManager(context: Application) : SpeechRecognizerManager(context) {
    var callbackRef: SpeechRecognitionCallback? = null
    var isListeningStarted = false
    var isCancelled = false
    var lastLanguage: VoiceLanguage? = null

    override fun isRecognitionAvailable(): Boolean = true

    override fun setCallback(callback: SpeechRecognitionCallback?) {
        this.callbackRef = callback
    }

    override fun startListening(language: VoiceLanguage) {
        isListeningStarted = true
        isCancelled = false
        lastLanguage = language
        callbackRef?.onReady()
    }

    override fun stopListening() {
        isListeningStarted = false
    }

    override fun cancel() {
        isListeningStarted = false
        isCancelled = true
    }
}

class FakeTextToSpeechManager(context: Application) : TextToSpeechManager(context) {
    var callbackRef: TtsCallback? = null
    var lastSpokenText: String? = null
    var lastUtteranceId: String? = null
    var isStopped = false

    override fun setCallback(callback: TtsCallback?) {
        this.callbackRef = callback
    }

    override fun speak(
        text: String,
        utteranceId: String,
        language: VoiceLanguage,
        rate: Float,
        pitch: Float,
        voiceId: String?
    ): Boolean {
        lastSpokenText = text
        lastUtteranceId = utteranceId
        isStopped = false
        callbackRef?.onSpeechStarted(utteranceId)
        return true
    }

    override fun speak(text: String, utteranceId: String, language: VoiceLanguage): Boolean {
        return speak(text, utteranceId, language, 1.0f, 1.0f, null)
    }

    override fun stop() {
        isStopped = true
    }

    override fun shutdown() {
        isStopped = true
    }
}

class FakeConversationRepository : ConversationRepository {
    val savedMessages = mutableListOf<ChatMessage>()

    override val allConversations: Flow<List<ConversationEntity>> = flowOf(emptyList())

    override suspend fun getConversation(id: String): ConversationEntity? = null

    override suspend fun getMessages(conversationId: String): List<ChatMessage> =
        savedMessages.filter { it.conversationId == conversationId }

    override fun getMessagesFlow(conversationId: String): Flow<List<ChatMessage>> =
        flowOf(savedMessages.filter { it.conversationId == conversationId })

    override suspend fun getRecentMessages(conversationId: String, limit: Int): List<ChatMessage> =
        savedMessages.filter { it.conversationId == conversationId }.takeLast(limit)

    override suspend fun createOrEnsureConversation(id: String, initialPrompt: String, defaultTitle: String): ConversationEntity {
        val now = System.currentTimeMillis()
        return ConversationEntity(
            id = id,
            title = defaultTitle,
            createdAt = now,
            updatedAt = now,
            lastMessagePreview = initialPrompt,
        )
    }

    override suspend fun saveUserTurn(conversationId: String, message: ChatMessage) {
        savedMessages.add(message)
    }

    override suspend fun saveAssistantTurn(conversationId: String, message: ChatMessage) {
        savedMessages.add(message)
    }

    override suspend fun deleteConversation(id: String): Boolean {
        return savedMessages.removeAll { it.conversationId == id }
    }

    override suspend fun renameConversation(id: String, newTitle: String) {}

    override suspend fun clearAllConversations(): Boolean {
        savedMessages.clear()
        return true
    }

    override fun getRecentConversations(limit: Int): Flow<List<ConversationEntity>> = flowOf(emptyList())

    override fun searchConversations(query: String): Flow<List<ConversationEntity>> = flowOf(emptyList())
}

class FakeMemoryRepository : com.evai.rifatcoder.data.repository.MemoryRepository {
    private val _enabled = kotlinx.coroutines.flow.MutableStateFlow(false)
    override val isMemoryEnabledFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _enabled
    override fun isMemoryEnabled(): Boolean = false
    override fun setMemoryEnabled(enabled: Boolean) { _enabled.value = enabled }
    override fun getConfirmedMemoriesFlow(): kotlinx.coroutines.flow.Flow<List<com.evai.rifatcoder.data.database.entity.MemoryEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override fun getSuggestedMemoriesFlow(): kotlinx.coroutines.flow.Flow<List<com.evai.rifatcoder.data.database.entity.MemoryEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override fun searchMemoriesFlow(query: String, category: String?): kotlinx.coroutines.flow.Flow<List<com.evai.rifatcoder.data.database.entity.MemoryEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override fun countConfirmedMemories(): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
    override suspend fun getMemoryById(id: String): com.evai.rifatcoder.data.database.entity.MemoryEntity? = null
    override suspend fun saveMemory(content: String, category: String, key: String, sourceConvId: String?, sourceMsgId: String?, importance: Int, isSuggested: Boolean) = Result.success(com.evai.rifatcoder.data.database.entity.MemoryEntity(id = "1", category = category, value = content))
    override suspend fun updateMemory(id: String, content: String, category: String, importance: Int) = Result.success(com.evai.rifatcoder.data.database.entity.MemoryEntity(id = id, category = category, value = content))
    override suspend fun deleteMemory(id: String): Boolean = true
    override suspend fun deleteAllMemories(): Boolean = true
    override suspend fun confirmSuggestion(id: String, editedContent: String?, editedCategory: String?): Boolean = true
    override suspend fun dismissSuggestion(id: String): Boolean = true
    override suspend fun getRelevantMemoriesForContext(prompt: String, maxCount: Int, maxChars: Int): List<com.evai.rifatcoder.data.database.entity.MemoryEntity> = emptyList()
    override fun buildMemoryContextPrompt(memories: List<com.evai.rifatcoder.data.database.entity.MemoryEntity>): String = ""
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoiceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application
    private lateinit var repository: FakeConversationRepository
    private lateinit var fakeAiEngine: FakeAiChatEngine
    private lateinit var fakeSpeechManager: FakeSpeechRecognizerManager
    private lateinit var fakeTtsManager: FakeTextToSpeechManager
    private lateinit var viewModel: VoiceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        com.evai.rifatcoder.data.voice.CentralVoiceStateManager.resetToIdle()
        com.evai.rifatcoder.data.glow.EdgeGlowStateHolder.resetToIdle()
        app = ApplicationProvider.getApplicationContext()
        repository = FakeConversationRepository()
        fakeAiEngine = FakeAiChatEngine()
        fakeSpeechManager = FakeSpeechRecognizerManager(app)
        fakeTtsManager = FakeTextToSpeechManager(app)

        viewModel = VoiceViewModel(
            application = app,
            speechManager = fakeSpeechManager,
            ttsManager = fakeTtsManager,
            chatEngine = fakeAiEngine,
            conversationRepository = repository,
            memoryRepository = FakeMemoryRepository(),
        )
    }

    @After
    fun tearDown() {
        com.evai.rifatcoder.data.voice.CentralVoiceStateManager.resetToIdle()
        com.evai.rifatcoder.data.glow.EdgeGlowStateHolder.resetToIdle()
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateIsReady() {
        val state = viewModel.uiState.value
        assertEquals(VoiceState.READY, state.voiceState)
        assertEquals(VoiceLanguage.ENGLISH, state.activeLanguage)
        assertTrue(state.isSpeechAvailable)
        assertFalse(state.isPermissionGranted)
        assertTrue(state.conversationId.isNotBlank())
    }

    @Test
    fun testLanguageToggleCyclesCorrectly() {
        assertEquals(VoiceLanguage.ENGLISH, viewModel.uiState.value.activeLanguage)
        viewModel.toggleLanguage()
        assertEquals(VoiceLanguage.BENGALI, viewModel.uiState.value.activeLanguage)
        viewModel.toggleLanguage()
        assertEquals(VoiceLanguage.ENGLISH, viewModel.uiState.value.activeLanguage)
    }

    @Test
    fun testPermissionGrantedStartsListening() {
        viewModel.setPermissionGranted(true)
        assertTrue(viewModel.uiState.value.isPermissionGranted)
        assertEquals(VoiceState.LISTENING, viewModel.uiState.value.voiceState)
        assertTrue(fakeSpeechManager.isListeningStarted)
    }

    @Test
    fun testPermissionDeniedSetsErrorState() {
        viewModel.setPermissionGranted(false)
        assertFalse(viewModel.uiState.value.isPermissionGranted)
        assertEquals(VoiceState.ERROR, viewModel.uiState.value.voiceState)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testSpeechFinalResultTransitionsToThinkingAndSpeaksResponse() = runTest(testDispatcher) {
        viewModel.setPermissionGranted(true)
        assertEquals(VoiceState.LISTENING, viewModel.uiState.value.voiceState)

        // Simulate final recognized speech
        fakeSpeechManager.callbackRef?.onFinalResult("What is the speed of light?")

        // State advances to THINKING
        assertEquals(VoiceState.THINKING, viewModel.uiState.value.voiceState)
        assertEquals("What is the speed of light?", viewModel.uiState.value.recognizedText)

        // Let coroutines execute
        advanceUntilIdle()

        // AI Engine called and assistant response spoken via TTS
        assertEquals(1, fakeAiEngine.promptCallCount)
        assertTrue(fakeAiEngine.lastPrompt?.contains("What is the speed of light?") == true)
        assertEquals("Hello from EVA AI!", viewModel.uiState.value.assistantResponseText)
        assertEquals(VoiceState.SPEAKING, viewModel.uiState.value.voiceState)
        assertEquals("Hello from EVA AI!", fakeTtsManager.lastSpokenText)

        // Verify persisted to repository
        val convId = viewModel.uiState.value.conversationId
        val messages = repository.getMessages(convId)
        assertEquals(2, messages.size)
        assertEquals(ChatRole.User, messages[0].role)
        assertEquals("What is the speed of light?", messages[0].content)
        assertEquals(ChatRole.Assistant, messages[1].role)
        assertEquals("Hello from EVA AI!", messages[1].content)
    }

    @Test
    fun testStopActiveSessionCancelsAndResetsToReady() = runTest(testDispatcher) {
        viewModel.setPermissionGranted(true)
        assertEquals(VoiceState.LISTENING, viewModel.uiState.value.voiceState)

        viewModel.stopActiveSession()

        assertEquals(VoiceState.READY, viewModel.uiState.value.voiceState)
        assertTrue(fakeSpeechManager.isCancelled)
        assertTrue(fakeTtsManager.isStopped)
        assertFalse(viewModel.uiState.value.isTtsSpeaking)
    }

    @Test
    fun testManualPromptSubmission() = runTest(testDispatcher) {
        viewModel.updateTranscriptionText("Hello EVA")
        assertEquals("Hello EVA", viewModel.uiState.value.recognizedText)

        viewModel.submitManualPrompt()
        advanceUntilIdle()

        assertEquals(1, fakeAiEngine.promptCallCount)
        assertTrue(fakeAiEngine.lastPrompt?.contains("Hello EVA") == true)
        assertEquals(VoiceState.SPEAKING, viewModel.uiState.value.voiceState)
    }

    @Test
    fun testReplayResponseSpeaksLastResponse() = runTest(testDispatcher) {
        fakeSpeechManager.callbackRef?.onFinalResult("Say something")
        advanceUntilIdle()

        assertEquals(VoiceState.SPEAKING, viewModel.uiState.value.voiceState)

        // Simulate speech completed
        fakeTtsManager.lastUtteranceId?.let { utteranceId ->
            fakeTtsManager.callbackRef?.onSpeechFinished(utteranceId)
        }
        assertEquals(VoiceState.READY, viewModel.uiState.value.voiceState)

        // Now trigger replay
        viewModel.replayResponse()
        assertEquals(VoiceState.SPEAKING, viewModel.uiState.value.voiceState)
        assertEquals("Hello from EVA AI!", fakeTtsManager.lastSpokenText)
    }
}
