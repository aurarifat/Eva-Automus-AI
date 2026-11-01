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

class OmniRouteProviderAdapter(
    private val client: OkHttpClient = defaultHttpClient(),
) : AiProviderAdapter {

    override val providerType: AiProviderType = AiProviderType.OMNIROUTE

    override suspend fun generateResponse(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        modelId: String,
        apiKey: String,
        baseUrl: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("OmniRoute API key is missing."))
        }
        val cleanModel = modelId.trim().ifEmpty { "omni-default" }
        val rawBase = (baseUrl ?: "https://api.omniroute.ai/v1/").trim()
        val normalizedBase = if (rawBase.endsWith("/")) rawBase else "$rawBase/"
        val endpointUrl = "${normalizedBase}chat/completions"

        try {
            val jsonPayload = buildOmniRoutePayload(prompt, conversationHistory, cleanModel)
            val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(endpointUrl)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(handleHttpError(response.code, responseBody, cleanModel))
                }

                val replyText = parseOmniRouteResponse(responseBody)
                Result.success(replyText)
            }
        } catch (e: IOException) {
            Result.failure(IOException("Network error while connecting to OmniRoute: ${e.localizedMessage}", e))
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
            prompt = "ping",
            conversationHistory = emptyList(),
            modelId = modelId,
            apiKey = apiKey,
            baseUrl = baseUrl,
        )
    }

    internal fun buildOmniRoutePayload(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        modelId: String,
    ): JSONObject {
        val root = JSONObject()
        root.put("model", modelId)

        val messagesArray = JSONArray()

        // System prompt
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", "You are EVA AI, a helpful, intelligent Android assistant.")
        messagesArray.put(systemMessage)

        // Filter valid prior turns (max 10 recent messages for context protection)
        val validHistory = conversationHistory
            .filter { it.status == MessageStatus.Complete || it.status == MessageStatus.Sent }
            .takeLast(10)

        for (msg in validHistory) {
            if (msg.content.isBlank()) continue
            val item = JSONObject()
            item.put("role", if (msg.role == ChatRole.User) "user" else "assistant")
            item.put("content", msg.content.trim())
            messagesArray.put(item)
        }

        // Add current user prompt
        val currentPromptItem = JSONObject()
        currentPromptItem.put("role", "user")
        currentPromptItem.put("content", prompt.trim())
        messagesArray.put(currentPromptItem)

        root.put("messages", messagesArray)
        return root
    }

    fun parseOmniRouteResponse(jsonString: String): String {
        val root = JSONObject(jsonString)
        val choices = root.optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            val error = root.optJSONObject("error")
            val errorMsg = error?.optString("message")
            if (!errorMsg.isNullOrBlank()) {
                throw IllegalStateException("OmniRoute error: $errorMsg")
            }
            throw IllegalStateException("OmniRoute returned an empty choices array.")
        }

        val firstChoice = choices.getJSONObject(0)
        val message = firstChoice.optJSONObject("message")
        val content = message?.optString("content")?.trim()

        if (content.isNullOrEmpty()) {
            throw IllegalStateException("OmniRoute returned an empty response content.")
        }

        return content
    }

    internal fun handleHttpError(code: Int, body: String, model: String): Exception {
        return when (code) {
            400 -> IllegalArgumentException("Bad request. Please verify the model ID ('$model') or parameters.")
            401, 403 -> SecurityException("Authentication failed. Please verify your OmniRoute API key.")
            404 -> IllegalArgumentException("OmniRoute endpoint or model '$model' was not found.")
            429 -> IllegalStateException("OmniRoute rate limit exceeded. Please wait a moment.")
            in 500..599 -> IOException("OmniRoute gateway/upstream service error (HTTP $code). Please try again.")
            else -> IOException("OmniRoute request failed with HTTP $code: ${parseErrorMessageFromBody(body)}")
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
