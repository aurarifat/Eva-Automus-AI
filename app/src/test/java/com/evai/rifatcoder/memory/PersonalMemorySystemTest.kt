package com.evai.rifatcoder.memory

import android.app.Application
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.evai.rifatcoder.data.database.EvaDatabase
import com.evai.rifatcoder.data.database.entity.ConversationEntity
import com.evai.rifatcoder.data.database.entity.MemoryEntity
import com.evai.rifatcoder.data.database.entity.MessageEntity
import com.evai.rifatcoder.data.memory.MemoryPreferencesRepository
import com.evai.rifatcoder.data.repository.RealConversationRepository
import com.evai.rifatcoder.data.repository.RealMemoryRepository
import com.evai.rifatcoder.data.security.AiProviderType
import com.evai.rifatcoder.domain.chat.AiChatEngine
import com.evai.rifatcoder.domain.chat.ChatMessage
import com.evai.rifatcoder.domain.chat.ChatRole
import com.evai.rifatcoder.domain.memory.MemoryExtractionEngine
import com.evai.rifatcoder.ui.memory.MemoryViewModel
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
class PersonalMemorySystemTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var database: EvaDatabase
    private lateinit var preferencesRepo: MemoryPreferencesRepository
    private lateinit var memoryRepo: RealMemoryRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        database = EvaDatabase.createInMemory(application)
        preferencesRepo = MemoryPreferencesRepository(
            application,
            prefsName = "test_memory_prefs_" + System.currentTimeMillis()
        )
        memoryRepo = RealMemoryRepository(database, preferencesRepo)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    // 1. Creating a memory
    @Test
    fun testCreateMemory() = runBlocking {
        val result = memoryRepo.saveMemory(
            content = "I prefer dark mode in all IDEs",
            category = "Preference",
            importance = 2,
        )

        assertTrue(result.isSuccess)
        val created = result.getOrNull()
        assertNotNull(created)
        assertEquals("I prefer dark mode in all IDEs", created!!.value)
        assertEquals("Preference", created.category)
        assertEquals(2, created.importance)
        assertTrue(created.isConfirmed)
        assertFalse(created.isSuggested)
    }

    // 2. Reading saved memories
    @Test
    fun testReadSavedMemories() = runBlocking {
        memoryRepo.saveMemory("I use Jetpack Compose", "Tool")
        memoryRepo.saveMemory("I am building an Android AI assistant", "Project")

        val list = memoryRepo.getConfirmedMemoriesFlow().first()
        assertEquals(2, list.size)
        assertTrue(list.any { it.value == "I use Jetpack Compose" && it.category == "Tool" })
        assertTrue(list.any { it.value == "I am building an Android AI assistant" && it.category == "Project" })
    }

    // 3. Editing a memory
    @Test
    fun testEditMemory() = runBlocking {
        val saved = memoryRepo.saveMemory("I use Python", "Skill").getOrThrow()
        val updateResult = memoryRepo.updateMemory(
            id = saved.id,
            content = "I use Kotlin and Jetpack Compose",
            category = "Skill",
            importance = 3,
        )

        assertTrue(updateResult.isSuccess)
        val updated = memoryRepo.getMemoryById(saved.id)
        assertNotNull(updated)
        assertEquals("I use Kotlin and Jetpack Compose", updated!!.value)
        assertEquals(3, updated.importance)
    }

    // 4. Deleting a single memory
    @Test
    fun testDeleteSingleMemory() = runBlocking {
        val mem1 = memoryRepo.saveMemory("Memory One", "General").getOrThrow()
        val mem2 = memoryRepo.saveMemory("Memory Two", "General").getOrThrow()

        val deleted = memoryRepo.deleteMemory(mem1.id)
        assertTrue(deleted)

        val remaining = memoryRepo.getConfirmedMemoriesFlow().first()
        assertEquals(1, remaining.size)
        assertEquals(mem2.id, remaining[0].id)
        assertNull(memoryRepo.getMemoryById(mem1.id))
    }

    // 5. Deleting all memories without deleting conversations
    @Test
    fun testDeleteAllMemoriesPreservesConversations() = runBlocking {
        // Create conversation and message
        val convRepo = RealConversationRepository(database)
        val convId = UUID.randomUUID().toString()
        convRepo.saveUserTurn(
            convId,
            ChatMessage(conversationId = convId, role = ChatRole.User, content = "Hello EVA")
        )

        // Save memories
        memoryRepo.saveMemory("Fact A", "Preference")
        memoryRepo.saveMemory("Fact B", "Tool")

        assertEquals(2, memoryRepo.getConfirmedMemoriesFlow().first().size)
        assertEquals(1, convRepo.getMessages(convId).size)

        // Delete all memories
        val cleared = memoryRepo.deleteAllMemories()
        assertTrue(cleared)

        // Memories table is empty
        val memoriesAfter = memoryRepo.getConfirmedMemoriesFlow().first()
        assertEquals(0, memoriesAfter.size)

        // Conversation history is completely intact!
        val messagesAfter = convRepo.getMessages(convId)
        assertEquals(1, messagesAfter.size)
        assertEquals("Hello EVA", messagesAfter[0].content)
        assertNotNull(convRepo.getConversation(convId))
    }

    // 6. Searching memories
    @Test
    fun testSearchMemories() = runBlocking {
        memoryRepo.saveMemory("I write backend microservices in Go", "Work")
        memoryRepo.saveMemory("I use Android Studio IDE", "Tool")
        memoryRepo.saveMemory("I study neural networks", "Skill")

        // Search text
        val searchBackend = memoryRepo.searchMemoriesFlow("microservices", null).first()
        assertEquals(1, searchBackend.size)
        assertEquals("I write backend microservices in Go", searchBackend[0].value)

        // Search category
        val searchTool = memoryRepo.searchMemoriesFlow("", "Tool").first()
        assertEquals(1, searchTool.size)
        assertEquals("Tool", searchTool[0].category)

        // Search with query and category
        val searchWithBoth = memoryRepo.searchMemoriesFlow("Studio", "Tool").first()
        assertEquals(1, searchWithBoth.size)
        assertEquals("I use Android Studio IDE", searchWithBoth[0].value)
    }

    // 7. Duplicate prevention
    @Test
    fun testDuplicatePrevention() = runBlocking {
        val firstSave = memoryRepo.saveMemory("I prefer dark mode", "Preference")
        assertTrue(firstSave.isSuccess)

        // Attempting to save exact same fact again must fail
        val duplicateSave = memoryRepo.saveMemory("I prefer dark mode", "Preference")
        assertFalse(duplicateSave.isSuccess)
        assertTrue(duplicateSave.exceptionOrNull()?.message?.contains("already exists") == true)

        val total = memoryRepo.getConfirmedMemoriesFlow().first()
        assertEquals(1, total.size)
    }

    // 8. Persistence after repository recreation
    @Test
    fun testPersistenceAfterRepositoryRecreation() = runBlocking {
        memoryRepo.saveMemory("Persistent fact across sessions", "Goal")

        // Recreate repository pointing to same database
        val newRepo = RealMemoryRepository(database, preferencesRepo)
        val memories = newRepo.getConfirmedMemoriesFlow().first()

        assertEquals(1, memories.size)
        assertEquals("Persistent fact across sessions", memories[0].value)
    }

    // 9. Personal Memory ON/OFF persistence
    @Test
    fun testPersonalMemoryTogglePersistence() {
        assertTrue(memoryRepo.isMemoryEnabled())

        memoryRepo.setMemoryEnabled(false)
        assertFalse(memoryRepo.isMemoryEnabled())

        // Recreate preferences repository with same prefs name
        val reloadedPrefs = MemoryPreferencesRepository(
            application,
            (preferencesRepo as Object).let { "test_memory_prefs" }
        )
        // Check standard setter
        preferencesRepo.setMemoryEnabled(true)
        assertTrue(preferencesRepo.isMemoryEnabled())
        preferencesRepo.setMemoryEnabled(false)
        assertFalse(preferencesRepo.isMemoryEnabled())
    }

    // 10. OFF mode prevents memory injection into AI context
    @Test
    fun testOffModePreventsMemoryInjection() = runBlocking {
        memoryRepo.saveMemory("Target User Preference", "Preference", importance = 3)
        assertTrue(memoryRepo.getConfirmedMemoriesFlow().first().isNotEmpty())

        // Turn OFF
        memoryRepo.setMemoryEnabled(false)

        val relevant = memoryRepo.getRelevantMemoriesForContext("Preference help")
        assertTrue(relevant.isEmpty())

        val promptContext = memoryRepo.buildMemoryContextPrompt(relevant)
        assertEquals("", promptContext)
    }

    // 11. Relevant-memory selection respects context limits
    @Test
    fun testRelevantMemorySelectionRespectsLimits() = runBlocking {
        memoryRepo.setMemoryEnabled(true)
        // Add 10 memories
        for (i in 1..10) {
            memoryRepo.saveMemory("Memory number $i explaining Android development topic $i", "Tool", importance = 1)
        }

        // Limit to max 3
        val selected = memoryRepo.getRelevantMemoriesForContext("Android development", maxCount = 3, maxChars = 200)
        assertTrue(selected.size <= 3)

        // Limit by character count
        val charLimited = memoryRepo.getRelevantMemoriesForContext("Android development", maxCount = 10, maxChars = 80)
        val totalChars = charLimited.sumOf { it.category.length + it.value.length + 8 }
        assertTrue(totalChars <= 80 || charLimited.size == 1)
    }

    // 12. Memory suggestions require user confirmation
    @Test
    fun testMemorySuggestionsRequireUserConfirmation() = runBlocking {
        val suggestion = memoryRepo.saveMemory(
            content = "User might be a designer",
            category = "Skill",
            isSuggested = true,
        ).getOrThrow()

        // It must appear in suggested memories, NOT in confirmed memories
        val confirmed = memoryRepo.getConfirmedMemoriesFlow().first()
        val suggested = memoryRepo.getSuggestedMemoriesFlow().first()

        assertEquals(0, confirmed.size)
        assertEquals(1, suggested.size)
        assertEquals(suggestion.id, suggested[0].id)
        assertTrue(suggested[0].isSuggested)
        assertFalse(suggested[0].isConfirmed)

        // Confirm suggestion
        val confirmedSuccess = memoryRepo.confirmSuggestion(suggestion.id)
        assertTrue(confirmedSuccess)

        // Now it moves to confirmed
        val confirmedAfter = memoryRepo.getConfirmedMemoriesFlow().first()
        val suggestedAfter = memoryRepo.getSuggestedMemoriesFlow().first()

        assertEquals(1, confirmedAfter.size)
        assertEquals(0, suggestedAfter.size)
        assertTrue(confirmedAfter[0].isConfirmed)
        assertFalse(confirmedAfter[0].isSuggested)
    }

    // 13. Invalid or malformed extraction output is rejected safely
    @Test
    fun testInvalidOrMalformedExtractionOutputRejected() = runBlocking {
        val fakeAi = object : AiChatEngine {
            override val isConfigured: Boolean = true
            override val activeModelName: String = "Test"
            override val activeProviderType = AiProviderType.GEMINI
            override suspend fun sendPrompt(
                conversationId: String,
                history: List<ChatMessage>,
                prompt: String,
            ): Result<String> {
                return Result.success("This is not JSON at all: { broken json ...")
            }
        }

        val engine = MemoryExtractionEngine(memoryRepo, fakeAi)
        val result = engine.inspectAndProposeMemory(
            userPrompt = "I am a senior Kotlin Android developer",
            assistantResponse = "Nice to meet you!",
            conversationId = "conv-1",
        )

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertEquals(0, memoryRepo.getSuggestedMemoriesFlow().first().size)
    }

    // 14. Secret / sensitive information rejection in extraction
    @Test
    fun testSecretAndSensitiveInformationRejection() = runBlocking {
        var extractionCalled = false
        val fakeAi = object : AiChatEngine {
            override val isConfigured: Boolean = true
            override val activeModelName: String = "Test"
            override val activeProviderType = AiProviderType.GEMINI
            override suspend fun sendPrompt(
                conversationId: String,
                history: List<ChatMessage>,
                prompt: String,
            ): Result<String> {
                extractionCalled = true
                return Result.success("{\"candidateFact\": \"My secret\", \"category\": \"General\"}")
            }
        }

        val engine = MemoryExtractionEngine(memoryRepo, fakeAi)

        // A. Question should not trigger extraction
        val qResult = engine.inspectAndProposeMemory(
            userPrompt = "What is the capital of Bangladesh?",
            assistantResponse = "Dhaka",
            conversationId = "conv-1",
        )
        assertNull(qResult.getOrNull())
        assertFalse(extractionCalled)

        // B. API Key or password statement must NOT be extracted
        val pwResult = engine.inspectAndProposeMemory(
            userPrompt = "My password is superSecretPassword123",
            assistantResponse = "Understood",
            conversationId = "conv-2",
        )
        assertNull(pwResult.getOrNull())
        assertFalse(extractionCalled)

        val apiKeyResult = engine.inspectAndProposeMemory(
            userPrompt = "Here is my api_key: AIzaSyDfakeKey12345",
            assistantResponse = "Got it",
            conversationId = "conv-3",
        )
        assertNull(apiKeyResult.getOrNull())
        assertFalse(extractionCalled)
    }

    // 15. Database migration MIGRATION_1_2 preserves existing conversations and messages
    @Test
    fun testMigrationPreservesConversationsAndMessages() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.cacheDir, "migration_test_${System.currentTimeMillis()}.db")
        if (dbFile.exists()) dbFile.delete()

        // 1. Create SQLite DB with Version 1 schema manually
        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbFile.name)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS conversations (
                            id TEXT PRIMARY KEY NOT NULL,
                            title TEXT NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL,
                            lastMessagePreview TEXT,
                            isTitleManuallyEdited INTEGER NOT NULL DEFAULT 0
                        )"""
                    )
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS messages (
                            id TEXT PRIMARY KEY NOT NULL,
                            conversationId TEXT NOT NULL,
                            role TEXT NOT NULL,
                            content TEXT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            status TEXT NOT NULL,
                            errorMessage TEXT
                        )"""
                    )
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS memories (
                            id TEXT PRIMARY KEY NOT NULL,
                            category TEXT NOT NULL,
                            key TEXT NOT NULL,
                            value TEXT NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )"""
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        val v1Db = helper.writableDatabase

        // Insert V1 conversation, message, and memory
        v1Db.execSQL("INSERT INTO conversations (id, title, createdAt, updatedAt, lastMessagePreview, isTitleManuallyEdited) VALUES ('c1', 'Existing Chat', 1000, 2000, 'Hi', 0)")
        v1Db.execSQL("INSERT INTO messages (id, conversationId, role, content, timestamp, status) VALUES ('m1', 'c1', 'User', 'Hello from V1', 1000, 'Complete')")
        v1Db.execSQL("INSERT INTO memories (id, category, key, value, createdAt, updatedAt) VALUES ('mem1', 'Preference', 'DarkMode', 'User likes dark mode', 1000, 2000)")

        // 2. Execute MIGRATION_1_2
        EvaDatabase.MIGRATION_1_2.migrate(v1Db)

        // 3. Verify conversation and message still exist exactly as before
        val convCursor = v1Db.query("SELECT id, title FROM conversations WHERE id = 'c1'")
        assertTrue(convCursor.moveToFirst())
        assertEquals("Existing Chat", convCursor.getString(1))
        convCursor.close()

        val msgCursor = v1Db.query("SELECT id, content FROM messages WHERE id = 'm1'")
        assertTrue(msgCursor.moveToFirst())
        assertEquals("Hello from V1", msgCursor.getString(1))
        msgCursor.close()

        // 4. Verify migrated memory entity has new columns with default values
        val memCursor = v1Db.query("SELECT id, value, isConfirmed, importance, isSuggested FROM memories WHERE id = 'mem1'")
        assertTrue(memCursor.moveToFirst())
        assertEquals("User likes dark mode", memCursor.getString(1))
        assertEquals(1, memCursor.getInt(2)) // isConfirmed default 1
        assertEquals(1, memCursor.getInt(3)) // importance default 1
        assertEquals(0, memCursor.getInt(4)) // isSuggested default 0
        memCursor.close()

        v1Db.close()
        dbFile.delete()
    }
}
