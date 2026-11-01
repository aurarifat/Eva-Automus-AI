package com.evai.rifatcoder.data.network

import com.evai.rifatcoder.data.security.AiProviderType
import com.evai.rifatcoder.domain.ai.AiProviderAdapter
import com.evai.rifatcoder.domain.chat.ChatMessage
import com.evai.rifatcoder.domain.chat.ChatRole
import com.evai.rifatcoder.domain.chat.MessageStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiProviderAdapter(
    private val client: OkHttpClient = defaultHttpClient(),
) : AiProviderAdapter {

    override val providerType: AiProviderType = AiProviderType.GEMINI

    override suspend fun generateResponse(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        modelId: String,
        apiKey: String,
        baseUrl: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is missing."))
        }
        val cleanModel = modelId.trim().ifEmpty { "gemini-3.5-flash" }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent"

        try {
            val jsonPayload = buildGeminiPayload(prompt, conversationHistory)
            val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", apiKey.trim())
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(handleHttpError(response.code, responseBody, cleanModel))
                }

                val replyText = parseGeminiResponse(responseBody)
                Result.success(replyText)
            }
        } catch (e: IOException) {
            Result.failure(IOException("Network error while connecting to Gemini: ${e.localizedMessage}", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun testConnection(
        apiKey: String,
        modelId: String,
        baseUrl: String?,
    ): Result<String> {
        return generateResponse(
            prompt = "Respond with 'OK' only.",
            conversationHistory = emptyList(),
            modelId = modelId,
            apiKey = apiKey,
        )
    }

    internal fun buildGeminiPayload(prompt: String, conversationHistory: List<ChatMessage>): JSONObject {
        val root = JSONObject()
        val contentsArray = JSONArray()

        // Filter valid prior turns (max 10 recent messages for context protection)
        val validHistory = conversationHistory
            .filter { it.status == MessageStatus.Complete || it.status == MessageStatus.Sent }
            .takeLast(10)

        for (msg in validHistory) {
            if (msg.content.isBlank()) continue
            val item = JSONObject()
            item.put("role", if (msg.role == ChatRole.User) "user" else "model")
            val parts = JSONArray()
            val textPart = JSONObject()
            textPart.put("text", msg.content.trim())
            parts.put(textPart)
            item.put("parts", parts)
            contentsArray.put(item)
        }

        // Add current user prompt
        val currentPromptItem = JSONObject()
        currentPromptItem.put("role", "user")
        val currentParts = JSONArray()
        val currentTextPart = JSONObject()
        currentTextPart.put("text", prompt.trim())
        currentParts.put(currentTextPart)
        currentPromptItem.put("parts", currentParts)
        contentsArray.put(currentPromptItem)

        root.put("contents", contentsArray)
        return root
    }

    fun parseGeminiResponse(jsonString: String): String {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            val promptFeedback = root.optJSONObject("promptFeedback")
            val blockReason = promptFeedback?.optString("blockReason")
            if (!blockReason.isNullOrBlank()) {
                throw IllegalStateException("Response blocked by Gemini safety filter ($blockReason).")
            }
            throw IllegalStateException("Gemini returned an empty candidate list.")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")

        if (parts == null || parts.length() == 0) {
            val finishReason = firstCandidate.optString("finishReason")
            throw IllegalStateException("Gemini response contained no text parts (finish reason: $finishReason).")
        }

        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val text = parts.getJSONObject(i).optString("text")
            sb.append(text)
        }

        val resultText = sb.toString().trim()
        if (resultText.isEmpty()) {
            throw IllegalStateException("Gemini returned blank text content.")
        }
        return resultText
    }

    internal fun handleHttpError(code: Int, body: String, model: String): Exception {
        return when (code) {
            400 -> IllegalArgumentException("Bad request. Please verify the Gemini model ID ('$model') or request format.")
            401, 403 -> SecurityException("Authentication failed. Please verify your Gemini API key.")
            404 -> IllegalArgumentException("Gemini model '$model' was not found or is unavailable.")
            429 -> IllegalStateException("Gemini API rate limit or quota exceeded. Please wait a moment.")
            in 500..599 -> IOException("Google Gemini service temporarily unavailable (HTTP $code). Please try again.")
            else -> IOException("Gemini request failed with HTTP $code: ${parseErrorMessageFromBody(body)}")
        }
    }

    private fun parseErrorMessageFromBody(body: String): String {
        return try {
            val obj = JSONObject(body)
            val error = obj.optJSONObject("error")
            error?.optString("message") ?: "Unknown error"
        } catch (_: Exception) {
            "Unknown response"
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        }
    }
}
