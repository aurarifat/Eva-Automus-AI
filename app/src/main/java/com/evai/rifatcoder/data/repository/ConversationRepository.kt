package com.evai.rifatcoder.data.repository

import com.evai.rifatcoder.data.database.EvaDatabase
import com.evai.rifatcoder.data.database.entity.ConversationEntity
import com.evai.rifatcoder.data.database.entity.MessageEntity
import com.evai.rifatcoder.domain.chat.ChatMessage
import com.evai.rifatcoder.domain.chat.ChatRole
import com.evai.rifatcoder.domain.chat.MessageStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ConversationRepository {
    val allConversations: Flow<List<ConversationEntity>>

    fun getRecentConversations(limit: Int = 5): Flow<List<ConversationEntity>>
    fun searchConversations(query: String): Flow<List<ConversationEntity>>
    suspend fun getConversation(id: String): ConversationEntity?
    fun getMessagesFlow(conversationId: String): Flow<List<ChatMessage>>
    suspend fun getMessages(conversationId: String): List<ChatMessage>
    suspend fun getRecentMessages(conversationId: String, limit: Int = 10): List<ChatMessage>

    suspend fun createOrEnsureConversation(
        id: String,
        initialPrompt: String,
        defaultTitle: String = "New Conversation",
    ): ConversationEntity

    suspend fun saveUserTurn(conversationId: String, message: ChatMessage)
    suspend fun saveAssistantTurn(conversationId: String, message: ChatMessage)
    suspend fun renameConversation(id: String, newTitle: String)
    suspend fun deleteConversation(id: String): Boolean
    suspend fun clearAllConversations(): Boolean
}

class RealConversationRepository(
    private val database: EvaDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ConversationRepository {

    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()

    override val allConversations: Flow<List<ConversationEntity>> =
        conversationDao.getAllConversationsFlow()

    override fun getRecentConversations(limit: Int): Flow<List<ConversationEntity>> =
        conversationDao.getRecentConversationsFlow(limit)

    override fun searchConversations(query: String): Flow<List<ConversationEntity>> {
        val trimmed = query.trim()
        return if (trimmed.isBlank()) {
            conversationDao.getAllConversationsFlow()
        } else {
            conversationDao.searchConversationsFlow(trimmed)
        }
    }

    override suspend fun getConversation(id: String): ConversationEntity? =
        withContext(ioDispatcher) {
            conversationDao.getConversationById(id)
        }

    override fun getMessagesFlow(conversationId: String): Flow<List<ChatMessage>> =
        messageDao.getMessagesForConversationFlow(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getMessages(conversationId: String): List<ChatMessage> =
        withContext(ioDispatcher) {
            messageDao.getMessagesForConversation(conversationId).map { it.toDomain() }
        }

    override suspend fun getRecentMessages(conversationId: String, limit: Int): List<ChatMessage> =
        withContext(ioDispatcher) {
            messageDao.getRecentMessagesForConversation(conversationId, limit).map { it.toDomain() }
        }

    override suspend fun createOrEnsureConversation(
        id: String,
        initialPrompt: String,
        defaultTitle: String,
    ): ConversationEntity = withContext(ioDispatcher) {
        val existing = conversationDao.getConversationById(id)
        if (existing != null) {
            return@withContext existing
        }

        val generatedTitle = generateTitleFromPrompt(initialPrompt, defaultTitle)
        val now = System.currentTimeMillis()
        val preview = initialPrompt.trim().take(120)

        val newEntity = ConversationEntity(
            id = id,
            title = generatedTitle,
            createdAt = now,
            updatedAt = now,
            lastMessagePreview = preview,
            isTitleManuallyEdited = false,
        )
        conversationDao.insertConversation(newEntity)
        newEntity
    }

    override suspend fun saveUserTurn(
        conversationId: String,
        message: ChatMessage,
    ) = withContext(ioDispatcher) {
        // Ensure conversation exists
        createOrEnsureConversation(
            id = conversationId,
            initialPrompt = message.content,
        )

        messageDao.insertMessage(message.toEntity(conversationId))

        // Update preview & timestamp on parent conversation
        val preview = message.content.trim().take(120)
        conversationDao.updateLastMessagePreview(
            id = conversationId,
            preview = preview,
            updatedAt = message.timestamp,
        )
    }

    override suspend fun saveAssistantTurn(
        conversationId: String,
        message: ChatMessage,
    ) = withContext(ioDispatcher) {
        // Late response isolation check: ensure conversation still exists in database.
        // If the user deleted the conversation while response was in-flight, do not resurrect!
        val parent = conversationDao.getConversationById(conversationId) ?: return@withContext

        messageDao.insertMessage(message.toEntity(conversationId))

        // If title hasn't been manually edited and current title is a fallback, generate a clean title
        if (!parent.isTitleManuallyEdited && (parent.title == "New Conversation" || parent.title.isBlank())) {
            val dynamicTitle = generateTitleFromPrompt(message.content, "Conversation")
            conversationDao.updateTitle(
                id = conversationId,
                newTitle = dynamicTitle,
                isManual = false,
                updatedAt = message.timestamp,
            )
        }

        val preview = message.content.trim().take(120)
        conversationDao.updateLastMessagePreview(
            id = conversationId,
            preview = preview,
            updatedAt = message.timestamp,
        )
    }

    override suspend fun renameConversation(id: String, newTitle: String) =
        withContext(ioDispatcher) {
            val cleanTitle = newTitle.trim().ifBlank { "Untitled Conversation" }
            conversationDao.updateTitle(
                id = id,
                newTitle = cleanTitle,
                isManual = true,
                updatedAt = System.currentTimeMillis(),
            )
        }

    override suspend fun deleteConversation(id: String): Boolean =
        withContext(ioDispatcher) {
            val rows = conversationDao.deleteConversationById(id)
            rows > 0
        }

    override suspend fun clearAllConversations(): Boolean =
        withContext(ioDispatcher) {
            conversationDao.deleteAllConversations()
            true
        }

    private fun generateTitleFromPrompt(prompt: String, fallback: String): String {
        val trimmed = prompt.lines().firstOrNull { it.isNotBlank() }?.trim() ?: ""
        if (trimmed.isBlank()) return fallback

        var cleaned = trimmed
        val prefixRegex = Regex(
            "^(hi|hello|hey|eva|please|tell me|can you help me understand|can you help me|can you|could you|how do i|how to|what is|explain|i want to know about)[,!?:;\\s]*\\s*",
            RegexOption.IGNORE_CASE
        )

        var matched = prefixRegex.find(cleaned)
        while (matched != null && matched.value.isNotEmpty() && matched.value.length < cleaned.length) {
            cleaned = cleaned.substring(matched.value.length).trimStart(',', '.', '!', '?', ':', ';', '-', ' ')
            matched = prefixRegex.find(cleaned)
        }

        cleaned = cleaned.trimStart(',', '.', '!', '?', ':', ';', '-', ' ')
        val titleCandidate = if (cleaned.isNotBlank()) cleaned else trimmed
        val truncated = titleCandidate.take(38).trim()

        return if (truncated.isNotBlank()) {
            truncated.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            fallback
        }
    }

    private fun MessageEntity.toDomain(): ChatMessage {
        val domainRole = when (role.lowercase()) {
            "assistant" -> ChatRole.Assistant
            "system" -> ChatRole.System
            else -> ChatRole.User
        }
        val domainStatus = when (status) {
            "Sent" -> MessageStatus.Sent
            "Generating" -> MessageStatus.Generating
            "NotConfigured" -> MessageStatus.NotConfigured
            "Failed" -> MessageStatus.Failed
            else -> MessageStatus.Complete
        }
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            role = domainRole,
            content = content,
            timestamp = timestamp,
            status = domainStatus,
            errorMessage = errorMessage,
        )
    }

    private fun ChatMessage.toEntity(convId: String): MessageEntity {
        return MessageEntity(
            id = id,
            conversationId = convId,
            role = when (role) {
                ChatRole.Assistant -> "assistant"
                ChatRole.System -> "system"
                ChatRole.User -> "user"
            },
            content = content,
            timestamp = timestamp,
            status = status.name,
            errorMessage = errorMessage,
        )
    }
}
