package com.evai.rifatcoder.domain.chat

import java.util.UUID

enum class ChatRole {
    User,
    Assistant,
    System,
}

enum class MessageStatus {
    Pending,
    Sent,
    Generating,
    Complete,
    NotConfigured,
    Failed,
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String = "conv-default",
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.Complete,
    val errorMessage: String? = null,
)
