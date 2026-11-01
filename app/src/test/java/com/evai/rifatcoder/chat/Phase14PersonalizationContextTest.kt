package com.evai.rifatcoder.chat

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.evai.rifatcoder.data.database.EvaDatabase
import com.evai.rifatcoder.data.memory.MemoryPreferencesRepository
import com.evai.rifatcoder.data.repository.RealMemoryRepository
import com.evai.rifatcoder.domain.chat.ChatContextBuilder
import com.evai.rifatcoder.domain.chat.ChatMessage
import com.evai.rifatcoder.domain.chat.ChatRole
import com.evai.rifatcoder.domain.chat.MessageStatus
import com.evai.rifatcoder.domain.language.DetectedLanguage
import com.evai.rifatcoder.domain.language.LanguageContextHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase14PersonalizationContextTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var database: EvaDatabase
    private lateinit var preferencesRepo: MemoryPreferencesRepository
    private lateinit var memoryRepo: RealMemoryRepository
    private lateinit var contextBuilder: ChatContextBuilder

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        database = EvaDatabase.createInMemory(application)
        preferencesRepo = MemoryPreferencesRepository(
            application,
            prefsName = "test_phase14_prefs_" + System.currentTimeMillis()
        )
        memoryRepo = RealMemoryRepository(database, preferencesRepo)
        contextBuilder = ChatContextBuilder(maxRecentTurns = 10, maxHistoryChars = 4000)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    // 1. Context ordering: Chronological order of recent conversation turns is preserved
    @Test
    fun testContextOrderingPreserved() = runBlocking {
        val messages = listOf(
            ChatMessage(id = "1", role = ChatRole.User, content = "First question", status = MessageStatus.Complete, timestamp = 1000L),
            ChatMessage(id = "2", role = ChatRole.Assistant, content = "First answer", status = MessageStatus.Complete, timestamp = 2000L),
            ChatMessage(id = "3", role = ChatRole.User, content = "Second question", status = MessageStatus.Complete, timestamp = 3000L),
            ChatMessage(id = "4", role = ChatRole.Assistant, content = "Second answer", status = MessageStatus.Complete, timestamp = 4000L),
        )

        val prepared = contextBuilder.buildContext(
            conversationHistory = messages,
            userPrompt = "Third question",
            relevantMemories = emptyList(),
            isIncognito = false,
        )

        assertEquals(4, prepared.boundedHistory.size)
        assertEquals("First question", prepared.boundedHistory[0].content)
        assertEquals("First answer", prepared.boundedHistory[1].content)
        assertEquals("Second question", prepared.boundedHistory[2].content)
        assertEquals("Second answer", prepared.boundedHistory[3].content)
        assertTrue(prepared.boundedHistory[0].timestamp <= prepared.boundedHistory[1].timestamp)
    }

    // 2. Relevant memory retrieval: matching memories retrieved, irrelevant excluded, Bengali Unicode supported
    @Test
    fun testRelevantMemoryRetrieval_EnglishAndBengali() = runBlocking {
        // Save English and Bengali memories
        memoryRepo.saveMemory(content = "My name is Aura RIFAT and I build Android apps", category = "Bio")
        memoryRepo.saveMemory(content = "আমি কোডিং করতে পছন্দ করি এবং জেটপ্যাক কম্পোজ ব্যবহার করি", category = "Skill")
        memoryRepo.saveMemory(content = "Favorite dish is Biryani and I avoid spicy soup", category = "Food")

        // Query regarding Android app development in English
        val englishMatches = memoryRepo.getRelevantMemoriesForContext("Tell me about my Android project", maxCount = 3)
        assertEquals(1, englishMatches.size)
        assertTrue(englishMatches[0].value.contains("Android apps"))

        // Query regarding Bengali coding preference
        val bengaliMatches = memoryRepo.getRelevantMemoriesForContext("তুমি কি জানো আমি কোডিং এবং কম্পোজ নিয়ে কি পছন্দ করি?", maxCount = 3)
        assertEquals(1, bengaliMatches.size)
        assertTrue(bengaliMatches[0].value.contains("জেটপ্যাক কম্পোজ"))

        // Query with completely unrelated keywords: non-matching memories must be filtered out
        val irrelevantMatches = memoryRepo.getRelevantMemoriesForContext("quantum astrophysics celestial telescope", maxCount = 3)
        assertTrue(irrelevantMatches.isEmpty())
    }

    // 3. Empty context handling: handles empty history and empty memories gracefully
    @Test
    fun testEmptyContextHandling() = runBlocking {
        val prepared = contextBuilder.buildContext(
            conversationHistory = emptyList(),
            userPrompt = "Hello EVA!",
            relevantMemories = emptyList(),
            isIncognito = false,
        )

        assertTrue(prepared.boundedHistory.isEmpty())
        assertNull(prepared.memoryContextPrompt)
        assertNotNull(prepared.finalPrompt)
        assertTrue(prepared.finalPrompt.contains("Hello EVA!"))
    }

    // 4. Incognito restrictions: no memories retrieved, no context leakage
    @Test
    fun testIncognitoModeRestrictions() = runBlocking {
        val saved = memoryRepo.saveMemory("Secret passcode is 987654321", "Secret").getOrNull()
        assertNotNull(saved)

        val memories = memoryRepo.getRelevantMemoriesForContext("What is my secret passcode?")

        val preparedIncognito = contextBuilder.buildContext(
            conversationHistory = emptyList(),
            userPrompt = "What is my secret passcode?",
            relevantMemories = memories,
            isIncognito = true,
        )

        // Memory must NOT be included in incognito mode
        assertNull(preparedIncognito.memoryContextPrompt)
        assertFalse(preparedIncognito.finalPrompt.contains("987654321"))
        assertTrue(preparedIncognito.finalPrompt.contains("Incognito Mode", ignoreCase = true))
    }

    // 5. Deleted memory prevention: deleted memories are never retrieved
    @Test
    fun testDeletedMemoryNotRetrieved() = runBlocking {
        val saved = memoryRepo.saveMemory("I live in Dhaka", "Location").getOrNull()
        assertNotNull(saved)

        // Retrieve before deletion
        val beforeDelete = memoryRepo.getRelevantMemoriesForContext("Where do I live in Dhaka?", maxCount = 2)
        assertEquals(1, beforeDelete.size)

        // Delete memory
        memoryRepo.deleteMemory(saved!!.id)

        // Query again
        val afterDelete = memoryRepo.getRelevantMemoriesForContext("Where do I live in Dhaka?", maxCount = 2)
        assertTrue(afterDelete.isEmpty())
    }

    // 6. Context size limits: history is truncated at maxRecentTurns
    @Test
    fun testContextSizeLimits() = runBlocking {
        val customBuilder = ChatContextBuilder(maxRecentTurns = 6, maxHistoryChars = 4000)
        val longList = mutableListOf<ChatMessage>()
        for (i in 1..20) {
            longList.add(ChatMessage(id = "u_$i", role = ChatRole.User, content = "User turn $i", status = MessageStatus.Complete, timestamp = i * 1000L))
            longList.add(ChatMessage(id = "a_$i", role = ChatRole.Assistant, content = "Assistant turn $i", status = MessageStatus.Complete, timestamp = i * 1000L + 500L))
        }

        // maxRecentTurns = 6
        val prepared = customBuilder.buildContext(
            conversationHistory = longList,
            userPrompt = "Latest prompt",
            relevantMemories = emptyList(),
            isIncognito = false,
        )

        assertEquals(6, prepared.boundedHistory.size)
        // Must contain the most recent turns (turns 18, 19, 20)
        assertEquals("User turn 18", prepared.boundedHistory[0].content)
        assertEquals("Assistant turn 20", prepared.boundedHistory[5].content)
    }

    // 7. Multilingual language detection: Bengali, Banglish, and English with code protection
    @Test
    fun testMultilingualDetectionAndCodeProtection() {
        // Bengali script detection
        val bengaliText = "কেমন আছেন? আজকের আবহাওয়া কেমন?"
        val detectedBengali = LanguageContextHelper.detectLanguage(bengaliText)
        assertEquals(DetectedLanguage.BENGALI, detectedBengali)

        // Banglish detection
        val banglishText = "apni kemon achen? ajke ki khobor?"
        val detectedBanglish = LanguageContextHelper.detectLanguage(banglishText)
        assertEquals(DetectedLanguage.BANGLISH, detectedBanglish)

        // English detection
        val englishText = "Can you help me build a Jetpack Compose ViewModel?"
        val detectedEnglish = LanguageContextHelper.detectLanguage(englishText)
        assertEquals(DetectedLanguage.ENGLISH, detectedEnglish)

        // Guidance contains strict preservation for code, URLs, and package name
        val instructions = LanguageContextHelper.buildLanguageGuidance(detectedBengali)
        assertTrue(instructions.contains("com.evai.rifatcoder"))
        assertTrue(instructions.contains("Do NOT translate"))
    }

    // 8. Duplicate memory prevention: identical or near-identical facts are rejected
    @Test
    fun testDuplicateMemoryPrevention() = runBlocking {
        val firstSave = memoryRepo.saveMemory("I use Kotlin for Android", "Skill")
        assertTrue(firstSave.isSuccess)

        // Duplicate memory with slight case/spacing differences
        val secondSave = memoryRepo.saveMemory("  I use Kotlin for Android  ", "Skill")
        assertTrue(secondSave.isFailure)
        assertTrue(secondSave.exceptionOrNull()?.message?.contains("already exists") == true)

        val allMemories = memoryRepo.getConfirmedMemoriesFlow().first()
        assertEquals(1, allMemories.size)
    }
}
