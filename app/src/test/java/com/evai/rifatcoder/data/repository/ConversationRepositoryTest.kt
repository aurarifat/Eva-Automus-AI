package com.evai.rifatcoder.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.evai.rifatcoder.data.database.EvaDatabase
import com.evai.rifatcoder.domain.chat.ChatMessage
import com.evai.rifatcoder.domain.chat.ChatRole
import com.evai.rifatcoder.domain.chat.MessageStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConversationRepositoryTest {

    private lateinit var database: EvaDatabase
    private lateinit var repository: ConversationRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = EvaDatabase.createInMemory(context)
        repository = RealConversationRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `auto-generates title from prompt and preserves manual edit`() = runBlocking {
        val convId = UUID.randomUUID().toString()
        val userTurn = ChatMessage(
            conversationId = convId,
            role = ChatRole.User,
            content = "Hello, can you help me understand Kotlin coroutines?",
        )

        repository.saveUserTurn(convId, userTurn)

        val conv = repository.getConversation(convId)
        assertNotNull(conv)
        assertFalse(conv!!.isTitleManuallyEdited)
        assertTrue(conv.title.contains("Kotlin coroutines", ignoreCase = true))

        // Manually rename title
        repository.renameConversation(convId, "My Custom Study Notes")
        val updated = repository.getConversation(convId)
        assertNotNull(updated)
        assertTrue(updated!!.isTitleManuallyEdited)
        assertEquals("My Custom Study Notes", updated.title)

        // Assistant response should NOT overwrite manually edited title
        val assistantTurn = ChatMessage(
            conversationId = convId,
            role = ChatRole.Assistant,
            content = "Sure! Kotlin coroutines are lightweight threads...",
        )
        repository.saveAssistantTurn(convId, assistantTurn)

        val finalCheck = repository.getConversation(convId)
        assertEquals("My Custom Study Notes", finalCheck?.title)
        assertTrue(finalCheck?.isTitleManuallyEdited == true)
    }

    @Test
    fun `cascade deletion deletes all associated messages`() = runBlocking {
        val convId = UUID.randomUUID().toString()
        val userMsg = ChatMessage(
            conversationId = convId,
            role = ChatRole.User,
            content = "First message",
        )
        val assistantMsg = ChatMessage(
            conversationId = convId,
            role = ChatRole.Assistant,
            content = "Second message",
        )

        repository.saveUserTurn(convId, userMsg)
        repository.saveAssistantTurn(convId, assistantMsg)

        val initialMessages = repository.getMessages(convId)
        assertEquals(2, initialMessages.size)

        // Delete conversation
        val deleted = repository.deleteConversation(convId)
        assertTrue(deleted)

        // Verify conversation is gone
        assertNull(repository.getConversation(convId))

        // Verify messages were CASCADE deleted
        val afterDeleteMessages = repository.getMessages(convId)
        assertTrue(afterDeleteMessages.isEmpty())
    }

    @Test
    fun `late response dropped if conversation is deleted while in flight`() = runBlocking {
        val convId = UUID.randomUUID().toString()
        val userMsg = ChatMessage(
            conversationId = convId,
            role = ChatRole.User,
            content = "Prompt before deletion",
        )
        repository.saveUserTurn(convId, userMsg)

        // Conversation is deleted by user while assistant is generating
        repository.deleteConversation(convId)
        assertNull(repository.getConversation(convId))

        // Assistant response arrives late
        val lateAssistantMsg = ChatMessage(
            conversationId = convId,
            role = ChatRole.Assistant,
            content = "I completed the prompt!",
        )
        repository.saveAssistantTurn(convId, lateAssistantMsg)

        // Ensure conversation was NOT resurrected
        assertNull(repository.getConversation(convId))
        assertTrue(repository.getMessages(convId).isEmpty())
    }

    @Test
    fun `recent messages retrieved up to limit in chronological order`() = runBlocking {
        val convId = UUID.randomUUID().toString()
        for (i in 1..15) {
            val role = if (i % 2 == 1) ChatRole.User else ChatRole.Assistant
            val msg = ChatMessage(
                conversationId = convId,
                role = role,
                content = "Message $i",
                timestamp = 1000L + i,
            )
            if (role == ChatRole.User) {
                repository.saveUserTurn(convId, msg)
            } else {
                repository.saveAssistantTurn(convId, msg)
            }
        }

        val all = repository.getMessages(convId)
        assertEquals(15, all.size)

        val recent10 = repository.getRecentMessages(convId, 10)
        assertEquals(10, recent10.size)
        // Check chronological order (ascending timestamps: 6 to 15)
        assertEquals("Message 6", recent10.first().content)
        assertEquals("Message 15", recent10.last().content)
    }

    @Test
    fun `search conversations filters by title and preview`() = runBlocking {
        val conv1 = UUID.randomUUID().toString()
        val conv2 = UUID.randomUUID().toString()

        repository.saveUserTurn(conv1, ChatMessage(conversationId = conv1, role = ChatRole.User, content = "Explore Android Architecture"))
        repository.saveUserTurn(conv2, ChatMessage(conversationId = conv2, role = ChatRole.User, content = "Bake Chocolate Cake"))

        val searchArch = repository.searchConversations("Architecture").first()
        assertEquals(1, searchArch.size)
        assertEquals(conv1, searchArch.first().id)

        val searchCake = repository.searchConversations("Cake").first()
        assertEquals(1, searchCake.size)
        assertEquals(conv2, searchCake.first().id)

        val searchEmpty = repository.searchConversations("").first()
        assertEquals(2, searchEmpty.size)
    }

    @Test
    fun `clear all conversations wipes database`() = runBlocking {
        val conv1 = UUID.randomUUID().toString()
        val conv2 = UUID.randomUUID().toString()

        repository.saveUserTurn(conv1, ChatMessage(conversationId = conv1, role = ChatRole.User, content = "Chat 1"))
        repository.saveUserTurn(conv2, ChatMessage(conversationId = conv2, role = ChatRole.User, content = "Chat 2"))

        assertEquals(2, repository.allConversations.first().size)

        repository.clearAllConversations()

        assertEquals(0, repository.allConversations.first().size)
    }
}
