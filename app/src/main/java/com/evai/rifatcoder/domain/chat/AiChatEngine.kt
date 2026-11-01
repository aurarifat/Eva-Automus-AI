package com.evai.rifatcoder.domain.chat

import com.evai.rifatcoder.data.network.GeminiProviderAdapter
import com.evai.rifatcoder.data.network.OmniRouteProviderAdapter
import com.evai.rifatcoder.data.security.AiProviderType
import com.evai.rifatcoder.data.security.SecureConfigStorage
import com.evai.rifatcoder.domain.ai.AiProviderAdapter

/**
 * Clean architectural boundary between Chat UI and real AI Provider layers.
 */
interface AiChatEngine {
    val isConfigured: Boolean
    val activeModelName: String
    val activeProviderType: AiProviderType

    suspend fun sendPrompt(
        conversationId: String,
        history: List<ChatMessage>,
        prompt: String,
    ): Result<String>

    suspend fun sendPrompt(
        conversationId: String,
        history: List<ChatMessage>,
        prompt: String,
        memoryContext: String?,
    ): Result<String> {
        return sendPrompt(conversationId, history, prompt)
    }
}

class AiProviderNotConfiguredException(
    message: String = "AI Provider (Gemini / OmniRoute) is not configured yet. Please configure an API key in Settings.",
) : Exception(message)

/**
 * Real production Chat Engine connecting to Gemini or OmniRoute depending on user configuration.
 */
class RealAiChatEngine(
    private val storage: SecureConfigStorage,
    private val geminiAdapter: AiProviderAdapter = GeminiProviderAdapter(),
    private val omniRouteAdapter: AiProviderAdapter = OmniRouteProviderAdapter(),
) : AiChatEngine {

    override val activeProviderType: AiProviderType
        get() = storage.getActiveProvider()

    override val isConfigured: Boolean
        get() = storage.hasApiKey(activeProviderType)

    override val activeModelName: String
        get() {
            return if (!isConfigured) {
                "${activeProviderType.displayName} (Not Configured)"
            } else {
                val model = when (activeProviderType) {
                    AiProviderType.GEMINI -> storage.getGeminiModelId()
                    AiProviderType.OMNIROUTE -> storage.getOmniRouteModelId()
                }
                "${activeProviderType.displayName} • $model"
            }
        }

    override suspend fun sendPrompt(
        conversationId: String,
        history: List<ChatMessage>,
        prompt: String,
    ): Result<String> {
        return sendPrompt(conversationId, history, prompt, null)
    }

    override suspend fun sendPrompt(
        conversationId: String,
        history: List<ChatMessage>,
        prompt: String,
        memoryContext: String?,
    ): Result<String> {
        val provider = activeProviderType
        val apiKey = when (provider) {
            AiProviderType.GEMINI -> storage.getGeminiApiKey()
            AiProviderType.OMNIROUTE -> storage.getOmniRouteApiKey()
        }

        if (apiKey.isNullOrBlank()) {
            return Result.failure(
                AiProviderNotConfiguredException(
                    "${provider.displayName} API key is not configured. Please open AI Connection to set up your key."
                )
            )
        }

        val adapter = when (provider) {
            AiProviderType.GEMINI -> geminiAdapter
            AiProviderType.OMNIROUTE -> omniRouteAdapter
        }

        val modelId = when (provider) {
            AiProviderType.GEMINI -> storage.getGeminiModelId()
            AiProviderType.OMNIROUTE -> storage.getOmniRouteModelId()
        }

        val baseUrl = if (provider == AiProviderType.OMNIROUTE) {
            storage.getOmniRouteBaseUrl()
        } else null

        val finalPrompt = if (!memoryContext.isNullOrBlank()) {
            "$memoryContext\n\n[USER MESSAGE]:\n$prompt"
        } else {
            prompt
        }

        return adapter.generateResponse(
            prompt = finalPrompt,
            conversationHistory = history,
            modelId = modelId,
            apiKey = apiKey,
            baseUrl = baseUrl,
        )
    }
}

/**
 * Lightweight local fallback implementation.
 */
class LocalChatEngineFoundation : AiChatEngine {
    override val isConfigured: Boolean = false
    override val activeProviderType: AiProviderType = AiProviderType.GEMINI
    override val activeModelName: String = "Provider Unconfigured (Phase 5)"

    override suspend fun sendPrompt(
        conversationId: String,
        history: List<ChatMessage>,
        prompt: String,
    ): Result<String> {
        return Result.failure(AiProviderNotConfiguredException())
    }
}
