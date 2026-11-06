package com.evai.rifatcoder.integration

import android.app.Application
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.evai.rifatcoder.data.database.EvaDatabase
import com.evai.rifatcoder.data.database.entity.MemoryEntity
import com.evai.rifatcoder.data.memory.MemoryPreferencesRepository
import com.evai.rifatcoder.data.repository.RealConversationRepository
import com.evai.rifatcoder.data.repository.RealMemoryRepository
import com.evai.rifatcoder.data.security.AiProviderType
import com.evai.rifatcoder.data.security.SecureConfigStorage
import com.evai.rifatcoder.data.voice.SpeechRecognizerManager
import com.evai.rifatcoder.data.voice.TextToSpeechManager
import com.evai.rifatcoder.data.voice.VoiceLanguage
import com.evai.rifatcoder.data.voice.VoicePreferencesRepository
import com.evai.rifatcoder.data.voice.VoiceState
import com.evai.rifatcoder.domain.chat.AiChatEngine
import com.evai.rifatcoder.domain.chat.ChatMessage
import com.evai.rifatcoder.domain.chat.ChatRole
import com.evai.rifatcoder.domain.chat.MessageStatus
import com.evai.rifatcoder.domain.memory.MemoryExtractionEngine
import com.evai.rifatcoder.ui.chat.ChatViewModel
import com.evai.rifatcoder.ui.voice.VoiceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase10IntegrationReliabilityTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var database: EvaDatabase
    private lateinit var convRepo: RealConversationRepository
    private lateinit var memoryPrefsRepo: MemoryPreferencesRepository
    private lateinit var memoryRepo: RealMemoryRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        com.evai.rifatcoder.data.voice.CentralVoiceStateManager.resetToIdle()
        com.evai.rifatcoder.data.glow.EdgeGlowStateHolder.resetToIdle()
        application = ApplicationProvider.getApplicationContext()
        database = EvaDatabase.createInMemory(application)
        convRepo = RealConversationRepository(database, Dispatchers.IO)
        memoryPrefsRepo = MemoryPreferencesRepository(
            application,
            prefsName = "test_p10_memory_prefs_" + System.currentTimeMillis()
        )
        memoryRepo = RealMemoryRepository(database, memoryPrefsRepo, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        com.evai.rifatcoder.data.voice.CentralVoiceStateManager.resetToIdle()
        com.evai.rifatcoder.data.glow.EdgeGlowStateHolder.resetToIdle()
        database.close()
        Dispatchers.resetMain()
    }

    // 1. Personal Memory OFF prevents chat memory injection
    @Test
    fun testPersonalMemoryOffPreventsChatMemoryInjection() = runTest(testDispatcher) {
        memoryRepo.saveMemory("I love Android Architecture Components", "Skill", importance = 3)
        memoryRepo.setMemoryEnabled(false)

        var capturedMemoryContext: String? = "UNSET"
        val fakeAi = object : AiChatEngine {
            override val isConfigured: Boolean = true
            override val activeModelName: String = "Test Model"
            override val activeProviderType = AiProviderType.GEMINI
            override suspend fun sendPrompt(
                conversationId: String,
                history: List<ChatMessage>,
                prompt: String,
            ): Result<String> = Result.success("OK")

            override suspend fun sendPrompt(
                conversationId: String,
                history: List<ChatMessage>,
                prompt: String,
                memoryContext: String?
            ): Result<String> {
                capturedMemoryContext = memoryContext
                return Result.success("Reply")
            }
        }

        val chatVm = ChatViewModel(
            application = application,
            chatEngine = fakeAi,
            conversationRepository = convRepo,
            memoryRepository = memoryRepo,
        )

        chatVm.updateComposerText("Tell me about architecture")
        chatVm.sendMessage()
        advanceUntilIdle()

        assertNull(capturedMemoryContext)
    }

    // 2. Personal Memory OFF prevents voice memory injection
    @Test
    fun testPersonalMemoryOffPreventsVoiceMemoryInjection() = runTest(testDispatcher) {
        memoryRepo.saveMemory("I use Compose exclusively", "Tool", importance = 3)
        memoryRepo.setMemoryEnabled(false)

        var capturedMemoryContext: String? = "UNSET"
        val fakeAi = object : AiChatEngine {
            override val isConfigured: Boolean = true
            override val activeModelName: String = "Test Model"
            override val activeProviderType = AiProviderType.GEMINI
            override suspend fun sendPrompt(
                conversationId: String,
                history: List<ChatMessage>,
                prompt: String,
            ): Result<String> = Result.success("OK")

            override suspend fun sendPrompt(
                conversationId: String,
                history: List<ChatMessage>,
                prompt: String,
                memoryContext: String?
            ): Result<String> {
                capturedMemoryContext = memoryContext
                return Result.success("Spoken reply")
            }
        }

        val voiceVm = VoiceViewModel(
            application = application,
            speechManager = SpeechRecognizerManager(application),
            ttsManager = object : TextToSpeechManager(application) {
                override fun speak(text: String, utteranceId: String, language: VoiceLanguage, rate: Float, pitch: Float, voiceId: String?) = true
            },
            chatEngine = fakeAi,
            conversationRepository = convRepo,
            preferencesRepository = VoicePreferencesRepository(application),
            memoryRepository = memoryRepo,
        )

        voiceVm.updateTranscriptionText("Compose advice")
        voiceVm.submitManualPrompt()
        advanceUntilIdle()

        assertNull(capturedMemoryContext)
    }

    // 3. Only confirmed memories enter AI context
    @Test
    fun testOnlyConfirmedMemoriesEnterAiContext() = runBlocking {
        memoryRepo.setMemoryEnabled(true)
        // Add confirmed memory
        memoryRepo.saveMemory("Confirmed fact: I write Kotlin", "Skill", isSuggested = false)
        // Add unconfirmed suggestion
        memoryRepo.saveMemory("Candidate fact: User might be studying iOS", "Skill", isSuggested = true)

        val relevant = memoryRepo.getRelevantMemoriesForContext("Kotlin programming")
        assertEquals(1, relevant.size)
        assertEquals("Confirmed fact: I write Kotlin", relevant[0].value)
        assertFalse(relevant[0].isSuggested)
        assertTrue(relevant[0].isConfirmed)
    }

    // 4. Suggested memories require confirmation
    @Test
    fun testSuggestedMemoriesRequireConfirmation() = runBlocking {
        val suggestion = memoryRepo.saveMemory(
            content = "User prefers concise answers",
            category = "Preference",
            isSuggested = true,
        ).getOrThrow()

        val confirmedBefore = memoryRepo.getConfirmedMemoriesFlow().first()
        assertEquals(0, confirmedBefore.size)

        // Confirm suggestion
        assertTrue(memoryRepo.confirmSuggestion(suggestion.id))

        val confirmedAfter = memoryRepo.getConfirmedMemoriesFlow().first()
        assertEquals(1, confirmedAfter.size)
        assertEquals("User prefers concise answers", confirmedAfter[0].value)
    }

    // 5. Malformed extraction results are rejected safely
    @Test
    fun testMalformedExtractionResultsRejected() = runBlocking {
        val fakeAi = object : AiChatEngine {
            override val isConfigured: Boolean = true
            override val activeModelName: String = "Test"
            override val activeProviderType = AiProviderType.GEMINI
            override suspend fun sendPrompt(
                conversationId: String,
                history: List<ChatMessage>,
                prompt: String,
            ): Result<String> {
                return Result.success("{ not a valid json syntax ...")
            }
        }

        val engine = MemoryExtractionEngine(memoryRepo, fakeAi)
        val result = engine.inspectAndProposeMemory(
            userPrompt = "I am preparing for an Android certification",
            assistantResponse = "Great goal!",
            conversationId = "conv-1",
        )

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertEquals(0, memoryRepo.getSuggestedMemoriesFlow().first().size)
    }

    // 6. Conversation History remains separate from Personal Memory
    @Test
    fun testConversationHistoryRemainsSeparateFromPersonalMemory() = runBlocking {
        val convId = UUID.randomUUID().toString()
        val userTurn = ChatMessage(conversationId = convId, role = ChatRole.User, content = "User message")
        convRepo.saveUserTurn(convId, userTurn)

        val memory = memoryRepo.saveMemory("User likes dogs", "Personal", sourceConvId = convId).getOrThrow()

        // Delete conversation
        convRepo.deleteConversation(convId)

        // Memory remains intact
        val storedMemory = memoryRepo.getMemoryById(memory.id)
        assertNotNull(storedMemory)
        assertEquals("User likes dogs", storedMemory!!.value)
    }

    // 7. Clearing memories preserves conversations
    @Test
    fun testClearingMemoriesPreservesConversations() = runBlocking {
        val convId = UUID.randomUUID().toString()
        convRepo.saveUserTurn(convId, ChatMessage(conversationId = convId, role = ChatRole.User, content = "Important task"))

        memoryRepo.saveMemory("Temporary fact 1", "General")
        memoryRepo.saveMemory("Temporary fact 2", "General")

        assertTrue(memoryRepo.deleteAllMemories())

        // Memories are cleared
        assertEquals(0, memoryRepo.getConfirmedMemoriesFlow().first().size)

        // Conversation history is intact
        val messages = convRepo.getMessages(convId)
        assertEquals(1, messages.size)
        assertEquals("Important task", messages[0].content)
    }

    // 8. Database migration preserves existing records
    @Test
    fun testDatabaseMigrationPreservesRecords() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.cacheDir, "p10_migration_test_${System.currentTimeMillis()}.db")
        if (dbFile.exists()) dbFile.delete()

        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbFile.name)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS conversations (id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, lastMessagePreview TEXT, isTitleManuallyEdited INTEGER NOT NULL DEFAULT 0)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS messages (id TEXT PRIMARY KEY NOT NULL, conversationId TEXT NOT NULL, role TEXT NOT NULL, content TEXT NOT NULL, timestamp INTEGER NOT NULL, status TEXT NOT NULL, errorMessage TEXT)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS memories (id TEXT PRIMARY KEY NOT NULL, category TEXT NOT NULL, key TEXT NOT NULL, value TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        val v1Db = helper.writableDatabase

        v1Db.execSQL("INSERT INTO conversations (id, title, createdAt, updatedAt, lastMessagePreview, isTitleManuallyEdited) VALUES ('c_test', 'V1 Title', 10, 20, 'Prev', 0)")
        v1Db.execSQL("INSERT INTO messages (id, conversationId, role, content, timestamp, status) VALUES ('m_test', 'c_test', 'User', 'Content', 10, 'Complete')")
        v1Db.execSQL("INSERT INTO memories (id, category, key, value, createdAt, updatedAt) VALUES ('mem_test', 'Tool', 'Key', 'Value', 10, 20)")

        EvaDatabase.MIGRATION_1_2.migrate(v1Db)

        val cursor = v1Db.query("SELECT id, title FROM conversations WHERE id = 'c_test'")
        assertTrue(cursor.moveToFirst())
        assertEquals("V1 Title", cursor.getString(1))
        cursor.close()

        val memCursor = v1Db.query("SELECT id, isConfirmed, importance FROM memories WHERE id = 'mem_test'")
        assertTrue(memCursor.moveToFirst())
        assertEquals(1, memCursor.getInt(1))
        assertEquals(1, memCursor.getInt(2))
        memCursor.close()

        v1Db.close()
        dbFile.delete()
    }

    // 9. Settings survive repository recreation
    @Test
    fun testSettingsSurviveRepositoryRecreation() {
        val testPrefsName = "p10_prefs_persist_test"
        val repo1 = MemoryPreferencesRepository(application, prefsName = testPrefsName)
        repo1.setMemoryEnabled(false)

        val repo2 = MemoryPreferencesRepository(application, prefsName = testPrefsName)
        assertFalse(repo2.isMemoryEnabled())

        repo2.setMemoryEnabled(true)
        val repo3 = MemoryPreferencesRepository(application, prefsName = testPrefsName)
        assertTrue(repo3.isMemoryEnabled())
    }

    // 10. Chat loading state resets after an error
    @Test
    fun testChatLoadingStateResetsAfterError() = runTest(testDispatcher) {
        val failingAi = object : AiChatEngine {
            override val isConfigured: Boolean = true
            override val activeModelName: String = "Test Model"
            override val activeProviderType = AiProviderType.GEMINI
            override suspend fun sendPrompt(
                conversationId: String,
                history: List<ChatMessage>,
                prompt: String,
            ): Result<String> {
                return Result.failure(java.io.IOException("Network connect timed out"))
            }
        }

        val chatVm = ChatViewModel(
            application = application,
            chatEngine = failingAi,
            conversationRepository = convRepo,
            memoryRepository = memoryRepo,
        )

        chatVm.updateComposerText("Test query")
        chatVm.sendMessage()
        for (i in 0 until 30) {
            if (!chatVm.uiState.value.isSending) break
            Thread.sleep(100)
            advanceUntilIdle()
        }

        assertFalse(chatVm.uiState.value.isSending)
        val lastMsg = chatVm.uiState.value.messages.last()
        assertEquals(MessageStatus.Failed, lastMsg.status)
        assertEquals("Network connect timed out", lastMsg.content)
    }

    // 11. Provider failures do not create false successful responses
    @Test
    fun testProviderFailuresDoNotCreateFalseSuccessfulResponses() = runTest(testDispatcher) {
        val unconfiguredAi = object : AiChatEngine {
            override val isConfigured: Boolean = false
            override val activeModelName: String = "Gemini (Not Configured)"
            override val activeProviderType = AiProviderType.GEMINI
            override suspend fun sendPrompt(
                conversationId: String,
                history: List<ChatMessage>,
                prompt: String,
            ): Result<String> {
                return Result.failure(IllegalArgumentException("API Key missing"))
            }
        }

        val chatVm = ChatViewModel(
            application = application,
            chatEngine = unconfiguredAi,
            conversationRepository = convRepo,
            memoryRepository = memoryRepo,
        )

        chatVm.updateComposerText("Hello")
        chatVm.sendMessage()
        for (i in 0 until 30) {
            if (chatVm.uiState.value.messages.size >= 2) break
            Thread.sleep(100)
            advanceUntilIdle()
        }

        val messages = chatVm.uiState.value.messages
        assertEquals(2, messages.size) // User sent turn + Failed assistant notification
        val assistantNotice = messages[1]
        assertEquals(ChatRole.Assistant, assistantNotice.role)
        assertEquals(MessageStatus.NotConfigured, assistantNotice.status)
    }

    // 12. Voice errors restore the appropriate UI state
    @Test
    fun testVoiceErrorsRestoreAppropriateUiState() = runTest(testDispatcher) {
        val failingAi = object : AiChatEngine {
            override val isConfigured: Boolean = true
            override val activeModelName: String = "Test"
            override val activeProviderType = AiProviderType.GEMINI
            override suspend fun sendPrompt(
                conversationId: String,
                history: List<ChatMessage>,
                prompt: String,
            ): Result<String> = Result.failure(RuntimeException("Speech synthesis failed"))
        }

        val voiceVm = VoiceViewModel(
            application = application,
            speechManager = SpeechRecognizerManager(application),
            ttsManager = object : TextToSpeechManager(application) {
                override fun speak(text: String, utteranceId: String, language: VoiceLanguage, rate: Float, pitch: Float, voiceId: String?) = false
            },
            chatEngine = failingAi,
            conversationRepository = convRepo,
            preferencesRepository = VoicePreferencesRepository(application),
            memoryRepository = memoryRepo,
        )

        voiceVm.updateTranscriptionText("Error prompt")
        voiceVm.submitManualPrompt()
        for (i in 0 until 30) {
            if (voiceVm.uiState.value.voiceState != VoiceState.THINKING) break
            Thread.sleep(100)
            advanceUntilIdle()
        }

        assertEquals(VoiceState.ERROR, voiceVm.uiState.value.voiceState)
        assertFalse(voiceVm.uiState.value.isTtsSpeaking)
        assertNotNull(voiceVm.uiState.value.errorMessage)
    }

    // 13. Search and filtering use real stored records
    @Test
    fun testSearchAndFilteringUseRealStoredRecords() = runBlocking {
        memoryRepo.saveMemory("Building responsive Android app in Jetpack Compose", "Project")
        memoryRepo.saveMemory("Using Room SQLite for database storage", "Tool")
        memoryRepo.saveMemory("Mastering Kotlin coroutines and Flow", "Skill")

        val result1 = memoryRepo.searchMemoriesFlow("Room", null).first()
        assertEquals(1, result1.size)
        assertEquals("Using Room SQLite for database storage", result1[0].value)

        val result2 = memoryRepo.searchMemoriesFlow("", "Skill").first()
        assertEquals(1, result2.size)
        assertEquals("Mastering Kotlin coroutines and Flow", result2[0].value)
    }

    // 14. Duplicate prompt submission is prevented where required
    @Test
    fun testDuplicatePromptSubmissionPreventedWhenSending() = runTest(testDispatcher) {
        var callCount = 0
        val slowAi = object : AiChatEngine {
            override val isConfigured: Boolean = true
            override val activeModelName: String = "Test"
            override val activeProviderType = AiProviderType.GEMINI
            override suspend fun sendPrompt(
                conversationId: String,
                history: List<ChatMessage>,
                prompt: String,
            ): Result<String> {
                callCount++
                return Result.success("Done")
            }
        }

        val chatVm = ChatViewModel(
            application = application,
            chatEngine = slowAi,
            conversationRepository = convRepo,
            memoryRepository = memoryRepo,
        )

        chatVm.updateComposerText("Once only")
        chatVm.sendMessage()
        // Immediate second tap while first is sending
        chatVm.sendMessage()

        for (i in 0 until 30) {
            if (callCount > 0) break
            Thread.sleep(100)
            advanceUntilIdle()
        }
        assertEquals(1, callCount)
    }

    // 15. Existing tests and repository methods continue to pass
    @Test
    fun testRepositoryMethodsFunctionConsistently() = runBlocking {
        val memory = memoryRepo.saveMemory("Test Consistency", "General").getOrThrow()
        val fetched = memoryRepo.getMemoryById(memory.id)
        assertNotNull(fetched)
        assertEquals("Test Consistency", fetched!!.value)

        assertTrue(memoryRepo.deleteMemory(memory.id))
        assertNull(memoryRepo.getMemoryById(memory.id))
    }
}
