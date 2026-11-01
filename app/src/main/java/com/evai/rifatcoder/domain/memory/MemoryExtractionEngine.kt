package com.evai.rifatcoder.domain.memory

import com.evai.rifatcoder.data.database.entity.MemoryEntity
import com.evai.rifatcoder.data.repository.MemoryRepository
import com.evai.rifatcoder.domain.chat.AiChatEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

/**
 * Cautious and transparent engine for proposing useful personal memory suggestions
 * from explicit statements made by the user during completed conversations.
 */
class MemoryExtractionEngine(
    private val memoryRepository: MemoryRepository,
    private val chatEngine: AiChatEngine,
) {

    /**
     * Inspects a completed user turn and proposes a candidate fact if the user
     * explicitly stated a personal preference, tool, skill, project, or goal.
     */
    suspend fun inspectAndProposeMemory(
        userPrompt: String,
        assistantResponse: String,
        conversationId: String,
        messageId: String? = null,
    ): Result<MemoryEntity?> = withContext(Dispatchers.IO) {
        // 1. Check if Personal Memory is enabled
        if (!memoryRepository.isMemoryEnabled()) {
            return@withContext Result.success(null)
        }

        // 2. Pre-filter candidate text for validity and safety
        val cleanPrompt = userPrompt.trim()
        if (cleanPrompt.length < 12) {
            return@withContext Result.success(null)
        }

        // Exclude questions
        if (isQuestion(cleanPrompt)) {
            return@withContext Result.success(null)
        }

        // Exclude secrets, tokens, credentials, or highly sensitive topics
        if (containsSecretsOrSensitiveData(cleanPrompt)) {
            return@withContext Result.success(null)
        }

        // Check if AI provider is configured
        if (!chatEngine.isConfigured) {
            return@withContext Result.success(null)
        }

        val extractionPrompt = buildExtractionPrompt(cleanPrompt)

        try {
            val responseResult = chatEngine.sendPrompt(
                conversationId = conversationId,
                history = emptyList(),
                prompt = extractionPrompt,
            )

            val rawResponse = responseResult.getOrNull() ?: return@withContext Result.success(null)
            val candidate = parseCandidateJson(rawResponse) ?: return@withContext Result.success(null)

            // Validate candidate fact
            if (candidate.fact.isBlank() || candidate.fact.length < 5) {
                return@withContext Result.success(null)
            }

            // Ensure extracted fact doesn't leak secrets
            if (containsSecretsOrSensitiveData(candidate.fact)) {
                return@withContext Result.success(null)
            }

            // Save as an unconfirmed suggestion
            val saveResult = memoryRepository.saveMemory(
                content = candidate.fact,
                category = candidate.category,
                key = candidate.fact.take(24).trimEnd(),
                sourceConvId = conversationId,
                sourceMsgId = messageId,
                importance = 1,
                isSuggested = true,
            )

            Result.success(saveResult.getOrNull())
        } catch (_: Exception) {
            // Fail safely without disrupting chat flow
            Result.success(null)
        }
    }

    private fun isQuestion(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.endsWith("?")) return true
        val lower = trimmed.lowercase(Locale.ROOT)
        val questionStarters = listOf("what ", "why ", "how ", "is there", "can you", "could you", "tell me", "explain ", "who is", "where is", "when did")
        return questionStarters.any { lower.startsWith(it) }
    }

    private fun containsSecretsOrSensitiveData(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        // Secrets & credentials patterns
        val secretKeywords = listOf(
            "password", "passwd", "api_key", "apikey", "secret_key", "bearer ",
            "access_token", "auth_token", "private_key", "id_rsa", "client_secret",
            "cvv", "credit card", "ssh-rsa"
        )
        if (secretKeywords.any { lower.contains(it) }) return true

        // Sensitive topics (health conditions, religion, political ballots, sexuality)
        val sensitiveKeywords = listOf(
            "medical diagnosis", "diagnosed with", "prescription",
            "political party", "voted for", "religious belief", "sexual orientation"
        )
        return sensitiveKeywords.any { lower.contains(it) }
    }

    private fun buildExtractionPrompt(userText: String): String {
        return """
You are a factual memory extraction module for EVA AI.
Analyze ONLY the explicit statement made by the user in this message:
"$userText"

Rules:
1. ONLY extract if the user made an explicit personal statement about their preferences, ongoing projects, developer tools, skills, or learning goals (e.g., "I code in Kotlin", "I prefer dark theme", "I am building an Android app").
2. Do NOT extract if the user was asking a question, discussing hypothetical scenarios, or quoting someone else.
3. NEVER extract passwords, API keys, credentials, or sensitive data.
4. Output MUST be strictly valid JSON in this exact structure with NO markdown around it:
{"candidateFact": "User prefers concise answers", "category": "Preference", "reason": "Explicit user statement"}
or if no personal fact was stated:
{"candidateFact": null}
""".trimIndent()
    }

    internal fun parseCandidateJson(rawJson: String): CandidateFact? {
        val cleaned = rawJson.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val obj = JSONObject(cleaned)
            val fact = obj.optString("candidateFact", "").takeIf { it.isNotBlank() && it != "null" }
                ?: return null
            val category = obj.optString("category", "General").ifBlank { "General" }
            val reason = obj.optString("reason", "")
            CandidateFact(fact = fact, category = category, reason = reason)
        } catch (_: Exception) {
            null
        }
    }

    data class CandidateFact(
        val fact: String,
        val category: String,
        val reason: String,
    )
}
