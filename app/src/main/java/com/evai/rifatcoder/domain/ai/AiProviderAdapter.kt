package com.evai.rifatcoder.domain.ai

import com.evai.rifatcoder.data.security.AiProviderType
import com.evai.rifatcoder.domain.chat.ChatMessage

interface AiProviderAdapter {
    val providerType: AiProviderType

    suspend fun generateResponse(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        modelId: String,
        apiKey: String,
        baseUrl: String? = null,
    ): Result<String>

    suspend fun testConnection(
        apiKey: String,
        modelId: String,
        baseUrl: String? = null,
    ): Result<String>
}
