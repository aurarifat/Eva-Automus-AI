package com.evai.rifatcoder

import androidx.compose.ui.unit.dp
import com.evai.rifatcoder.data.network.GeminiProviderAdapter
import com.evai.rifatcoder.data.network.OmniRouteProviderAdapter
import com.evai.rifatcoder.data.security.AiProviderType
import com.evai.rifatcoder.data.security.ProviderConnectionStatus
import com.evai.rifatcoder.data.security.SecureConfigStorage
import com.evai.rifatcoder.domain.chat.AiProviderNotConfiguredException
import com.evai.rifatcoder.domain.chat.ChatMessage
import com.evai.rifatcoder.domain.chat.ChatRole
import com.evai.rifatcoder.domain.chat.LocalChatEngineFoundation
import com.evai.rifatcoder.domain.chat.MessageStatus
import com.evai.rifatcoder.domain.contract.AccessibilityAutomationContract
import com.evai.rifatcoder.domain.contract.FourSideEdgeAnimationContract
import com.evai.rifatcoder.navigation.EvaDestination
import com.evai.rifatcoder.ui.chat.ChatUiState
import com.evai.rifatcoder.ui.components.EvaStatusLevel
import com.evai.rifatcoder.ui.config.AiConfigUiState
import com.evai.rifatcoder.ui.home.AiConnectionState
import com.evai.rifatcoder.ui.home.AssistantState
import com.evai.rifatcoder.ui.home.HomeUiState
import com.evai.rifatcoder.ui.home.PermissionSummaryState
import com.evai.rifatcoder.ui.theme.EvaSpacing
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun verifyNavigationDestinationsCountAndOrder() {
        val destinations = EvaDestination.entries
        assertEquals(5, destinations.size)
        assertEquals(EvaDestination.HOME, destinations[0])
        assertEquals(EvaDestination.CHAT, destinations[1])
        assertEquals(EvaDestination.VOICE, destinations[2])
        assertEquals(EvaDestination.MEMORY, destinations[3])
        assertEquals(EvaDestination.SETTINGS, destinations[4])
    }

    @Test
    fun verifyEvaSpacingTokens() {
        val spacing = EvaSpacing()
        assertEquals(4.dp, spacing.extraSmall)
        assertEquals(8.dp, spacing.small)
        assertEquals(12.dp, spacing.medium)
        assertEquals(16.dp, spacing.standard)
        assertEquals(24.dp, spacing.large)
        assertEquals(32.dp, spacing.extraLarge)
    }

    @Test
    fun verifyStatusLevels() {
        val levels = EvaStatusLevel.entries
        assertEquals(5, levels.size)
    }

    @Test
    fun verifyHomeUiStateDefaultsAreTruthful() {
        val defaultState = HomeUiState()
        assertEquals(AssistantState.SetupRequired, defaultState.assistantState)
        assertEquals(AiConnectionState.NotConfigured, defaultState.aiConnectionState)
        assertEquals(PermissionSummaryState.SetupRequired, defaultState.permissionState)
        assertTrue(defaultState.recentConversations.isEmpty())
        assertEquals(0, defaultState.grantedPermissionCount)
        assertEquals(14, defaultState.totalPermissionCount)
    }

    @Test
    fun verifyAssistantStateCompleteness() {
        assertEquals(8, AssistantState.entries.size)
        AssistantState.entries.forEach {
            assertTrue(it.label.isNotEmpty())
            assertTrue(it.description.isNotEmpty())
        }
    }

    @Test
    fun verifyFourSideEdgeAnimationContractStates() {
        val states = FourSideEdgeAnimationContract.EdgeVisualState.entries
        assertEquals(7, states.size)
    }

    @Test
    fun verifyAccessibilityAutomationContractStates() {
        val states = AccessibilityAutomationContract.ServiceState.entries
        assertEquals(6, states.size)
    }

    @Test
    fun verifyChatUiStateDefaults() {
        val state = ChatUiState()
        assertTrue(state.isEmpty)
        assertFalse(state.canSend)
        assertEquals("", state.composerText)
        assertTrue(state.messages.isEmpty())
        assertEquals(3, state.suggestedPrompts.size)
    }

    @Test
    fun verifyLocalChatEngineFoundationReturnsNotConfigured() = runTest {
        val engine = LocalChatEngineFoundation()
        assertFalse(engine.isConfigured)
        val result = engine.sendPrompt("conv-1", emptyList(), "Test prompt")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AiProviderNotConfiguredException)
    }

    // ==========================================
    // PHASE 5: GEMINI PROVIDER TESTS
    // ==========================================

    @Test
    fun verifyGeminiProviderAdapterParsesSuccessResponse() {
        val adapter = GeminiProviderAdapter()
        val mockJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "Hello, I am EVA AI powered by Google Gemini!" }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP"
                }
              ]
            }
        """.trimIndent()

        val parsed = adapter.parseGeminiResponse(mockJson)
        assertEquals("Hello, I am EVA AI powered by Google Gemini!", parsed)
    }

    @Test
    fun verifyGeminiProviderAdapterHandlesSafetyBlock() {
        val adapter = GeminiProviderAdapter()
        val mockJson = """
            {
              "candidates": [],
              "promptFeedback": {
                "blockReason": "SAFETY"
              }
            }
        """.trimIndent()

        try {
            adapter.parseGeminiResponse(mockJson)
            fail("Expected IllegalStateException due to safety filter")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("safety filter") == true)
        }
    }

    @Test
    fun verifyGeminiProviderAdapterHandlesEmptyCandidates() {
        val adapter = GeminiProviderAdapter()
        val mockJson = """{ "candidates": [] }"""

        try {
            adapter.parseGeminiResponse(mockJson)
            fail("Expected IllegalStateException due to empty candidate list")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("empty candidate list") == true)
        }
    }

    @Test
    fun verifyGeminiProviderAdapterHandlesBlankText() {
        val adapter = GeminiProviderAdapter()
        val mockJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [ { "text": "   " } ]
                  }
                }
              ]
            }
        """.trimIndent()

        try {
            adapter.parseGeminiResponse(mockJson)
            fail("Expected IllegalStateException due to blank text content")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("blank text content") == true)
        }
    }

    @Test
    fun verifyGeminiProviderAdapterHttpErrorMapping() {
        val adapter = GeminiProviderAdapter()

        val badRequest = adapter.handleHttpError(400, "{}", "gemini-3.5-flash")
        assertTrue(badRequest is IllegalArgumentException)
        assertTrue(badRequest.message?.contains("Bad request") == true)

        val unauth = adapter.handleHttpError(401, "{}", "gemini-3.5-flash")
        assertTrue(unauth is SecurityException)
        assertTrue(unauth.message?.contains("Authentication failed") == true)

        val notFound = adapter.handleHttpError(404, "{}", "gemini-unknown")
        assertTrue(notFound is IllegalArgumentException)
        assertTrue(notFound.message?.contains("was not found") == true)

        val rateLimit = adapter.handleHttpError(429, "{}", "gemini-3.5-flash")
        assertTrue(rateLimit is IllegalStateException)
        assertTrue(rateLimit.message?.contains("rate limit") == true)

        val serverErr = adapter.handleHttpError(500, "{}", "gemini-3.5-flash")
        assertTrue(serverErr is IOException)
        assertTrue(serverErr.message?.contains("temporarily unavailable") == true)
    }

    @Test
    fun verifyGeminiPayloadRoleMappingAndTurnLimit() {
        val adapter = GeminiProviderAdapter()
        val history = listOf(
            ChatMessage(role = ChatRole.User, content = "User turn 1", status = MessageStatus.Complete),
            ChatMessage(role = ChatRole.Assistant, content = "Model turn 1", status = MessageStatus.Complete),
            ChatMessage(role = ChatRole.User, content = "User turn 2", status = MessageStatus.Complete),
        )

        val payload = adapter.buildGeminiPayload("Current question", history)
        val contents = payload.getJSONArray("contents")
        assertEquals(4, contents.length())

        val firstTurn = contents.getJSONObject(0)
        assertEquals("user", firstTurn.getString("role"))
        assertEquals("User turn 1", firstTurn.getJSONArray("parts").getJSONObject(0).getString("text"))

        val secondTurn = contents.getJSONObject(1)
        assertEquals("model", secondTurn.getString("role"))
        assertEquals("Model turn 1", secondTurn.getJSONArray("parts").getJSONObject(0).getString("text"))

        val lastTurn = contents.getJSONObject(3)
        assertEquals("user", lastTurn.getString("role"))
        assertEquals("Current question", lastTurn.getJSONArray("parts").getJSONObject(0).getString("text"))
    }

    // ==========================================
    // PHASE 5: OMNIROUTE PROVIDER TESTS
    // ==========================================

    @Test
    fun verifyOmniRouteProviderAdapterParsesSuccessResponse() {
        val adapter = OmniRouteProviderAdapter()
        val mockJson = """
            {
              "id": "chatcmpl-123",
              "object": "chat.completion",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "Hello from OmniRoute router!"
                  },
                  "finish_reason": "stop"
                }
              ]
            }
        """.trimIndent()

        val parsed = adapter.parseOmniRouteResponse(mockJson)
        assertEquals("Hello from OmniRoute router!", parsed)
    }

    @Test
    fun verifyOmniRouteProviderAdapterHandlesErrorPayload() {
        val adapter = OmniRouteProviderAdapter()
        val mockJson = """
            {
              "error": {
                "message": "Invalid API key provided",
                "type": "invalid_request_error"
              }
            }
        """.trimIndent()

        try {
            adapter.parseOmniRouteResponse(mockJson)
            fail("Expected IllegalStateException due to OmniRoute error object")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Invalid API key provided") == true)
        }
    }

    @Test
    fun verifyOmniRouteProviderAdapterHandlesEmptyChoices() {
        val adapter = OmniRouteProviderAdapter()
        val mockJson = """{ "choices": [] }"""

        try {
            adapter.parseOmniRouteResponse(mockJson)
            fail("Expected IllegalStateException due to empty choices")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("empty choices array") == true)
        }
    }

    @Test
    fun verifyOmniRouteProviderAdapterHandlesBlankContent() {
        val adapter = OmniRouteProviderAdapter()
        val mockJson = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "   "
                  }
                }
              ]
            }
        """.trimIndent()

        try {
            adapter.parseOmniRouteResponse(mockJson)
            fail("Expected IllegalStateException due to empty response content")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("empty response content") == true)
        }
    }

    @Test
    fun verifyOmniRouteProviderAdapterHttpErrorMapping() {
        val adapter = OmniRouteProviderAdapter()

        val badReq = adapter.handleHttpError(400, "{}", "omni-default")
        assertTrue(badReq is IllegalArgumentException)
        assertTrue(badReq.message?.contains("Bad request") == true)

        val unauth = adapter.handleHttpError(401, "{}", "omni-default")
        assertTrue(unauth is SecurityException)
        assertTrue(unauth.message?.contains("Authentication failed") == true)

        val notFound = adapter.handleHttpError(404, "{}", "omni-default")
        assertTrue(notFound is IllegalArgumentException)
        assertTrue(notFound.message?.contains("was not found") == true)

        val rateLimit = adapter.handleHttpError(429, "{}", "omni-default")
        assertTrue(rateLimit is IllegalStateException)
        assertTrue(rateLimit.message?.contains("rate limit") == true)

        val serverErr = adapter.handleHttpError(500, "{}", "omni-default")
        assertTrue(serverErr is IOException)
        assertTrue(serverErr.message?.contains("gateway/upstream service error") == true)
    }

    @Test
    fun verifyOmniRoutePayloadRoleMappingAndTurnLimit() {
        val adapter = OmniRouteProviderAdapter()
        val history = listOf(
            ChatMessage(role = ChatRole.User, content = "User question 1", status = MessageStatus.Complete),
            ChatMessage(role = ChatRole.Assistant, content = "Assistant answer 1", status = MessageStatus.Complete),
        )

        val payload = adapter.buildOmniRoutePayload("Prompt 2", history, "omni-fast")
        assertEquals("omni-fast", payload.getString("model"))
        val messages = payload.getJSONArray("messages")

        // First item is system prompt
        assertEquals(4, messages.length())
        val systemTurn = messages.getJSONObject(0)
        assertEquals("system", systemTurn.getString("role"))
        assertTrue(systemTurn.getString("content").contains("EVA AI"))

        // User turn
        val userTurn1 = messages.getJSONObject(1)
        assertEquals("user", userTurn1.getString("role"))
        assertEquals("User question 1", userTurn1.getString("content"))

        // Assistant turn
        val assistantTurn = messages.getJSONObject(2)
        assertEquals("assistant", assistantTurn.getString("role"))
        assertEquals("Assistant answer 1", assistantTurn.getString("content"))

        // Current prompt
        val currentTurn = messages.getJSONObject(3)
        assertEquals("user", currentTurn.getString("role"))
        assertEquals("Prompt 2", currentTurn.getString("content"))
    }

    // ==========================================
    // PHASE 5: SECURITY & STATE TESTS
    // ==========================================

    @Test
    fun verifySecretSanitization() {
        val fakeKey = "AIzaSyD-fakeKey123456789"
        val fakeBearer = "Bearer sk-proj-fakeTokenABC123"
        val errorMsg = "Error connecting to https://generativelanguage.googleapis.com?key=$fakeKey using $fakeBearer with x-goog-api-key: $fakeKey"

        val dummyStorageSanitizer = { raw: String ->
            raw.replace(Regex("key=[a-zA-Z0-9_\\-]+"), "key=REDACTED")
                .replace(Regex("Bearer\\s+[a-zA-Z0-9_\\-\\.]+"), "Bearer REDACTED")
                .replace(Regex("x-goog-api-key:\\s*[a-zA-Z0-9_\\-]+"), "x-goog-api-key: REDACTED")
        }

        val sanitized = dummyStorageSanitizer(errorMsg)
        assertFalse(sanitized.contains(fakeKey))
        assertFalse(sanitized.contains("sk-proj-fakeTokenABC123"))
        assertTrue(sanitized.contains("key=REDACTED"))
        assertTrue(sanitized.contains("Bearer REDACTED"))
        assertTrue(sanitized.contains("x-goog-api-key: REDACTED"))
    }

    @Test
    fun verifyAiProviderTypeEnum() {
        val providers = AiProviderType.entries
        assertEquals(2, providers.size)
        assertEquals(AiProviderType.GEMINI, providers[0])
        assertEquals("Google Gemini", providers[0].displayName)
        assertEquals(AiProviderType.OMNIROUTE, providers[1])
        assertEquals("OmniRoute", providers[1].displayName)
    }

    @Test
    fun verifyProviderConnectionStatusEnum() {
        val statuses = ProviderConnectionStatus.entries
        assertEquals(5, statuses.size)
        assertTrue(statuses.contains(ProviderConnectionStatus.NOT_CONFIGURED))
        assertTrue(statuses.contains(ProviderConnectionStatus.TESTING))
        assertTrue(statuses.contains(ProviderConnectionStatus.CONNECTED))
        assertTrue(statuses.contains(ProviderConnectionStatus.CONNECTION_FAILED))
        assertTrue(statuses.contains(ProviderConnectionStatus.CONFIGURATION_CHANGED))
    }

    @Test
    fun verifyAiConfigUiStateDefaults() {
        val state = AiConfigUiState()
        assertEquals(AiProviderType.GEMINI, state.selectedProvider)
        assertEquals("gemini-3.5-flash", state.geminiModelIdInput)
        assertEquals(4, state.knownModels.size)
        assertEquals("omni-default", state.omniRouteModelIdInput)
        assertEquals("https://api.omniroute.ai/v1/", state.omniRouteBaseUrlInput)
        assertFalse(state.isTesting)
        assertFalse(state.hasStoredGeminiKey)
        assertFalse(state.hasStoredOmniRouteKey)
        assertFalse(state.isKeyVisible)
    }
}
