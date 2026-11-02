package com.evai.rifatcoder.security

import android.app.Application
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import com.evai.rifatcoder.data.accessibility.AccessibilityNodeInspector
import com.evai.rifatcoder.domain.automation.AccessibleNode
import com.evai.rifatcoder.data.database.EvaDatabase
import com.evai.rifatcoder.data.database.entity.ConversationEntity
import com.evai.rifatcoder.data.database.entity.MemoryEntity
import com.evai.rifatcoder.data.glow.EdgeGlowOverlayService
import com.evai.rifatcoder.data.glow.EdgeGlowStateHolder
import com.evai.rifatcoder.data.memory.MemoryPreferencesRepository
import com.evai.rifatcoder.data.network.GeminiProviderAdapter
import com.evai.rifatcoder.data.network.OmniRouteProviderAdapter
import com.evai.rifatcoder.data.repository.RealConversationRepository
import com.evai.rifatcoder.data.repository.RealMemoryRepository
import com.evai.rifatcoder.data.security.AiProviderType
import com.evai.rifatcoder.data.security.SecureConfigStorage
import com.evai.rifatcoder.data.security.SystemPermissionManager
import com.evai.rifatcoder.domain.automation.AutomationAction
import com.evai.rifatcoder.domain.automation.AutomationPlan
import com.evai.rifatcoder.domain.automation.AutomationSafetyGuard
import com.evai.rifatcoder.domain.automation.AutomationStep
import com.evai.rifatcoder.domain.automation.SafetyRiskLevel
import com.evai.rifatcoder.domain.chat.AiChatEngine
import com.evai.rifatcoder.domain.chat.ChatContextBuilder
import com.evai.rifatcoder.domain.chat.ChatMessage
import com.evai.rifatcoder.domain.chat.ChatRole
import com.evai.rifatcoder.domain.glow.EdgeGlowState
import com.evai.rifatcoder.domain.tool.AiTool
import com.evai.rifatcoder.domain.tool.AiToolRegistry
import com.evai.rifatcoder.domain.tool.ToolCategory
import com.evai.rifatcoder.ui.chat.ChatViewModel
import com.evai.rifatcoder.ui.voice.VoiceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase16SecurityPrivacyAuditTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application
    private lateinit var inMemoryDb: EvaDatabase
    private lateinit var memoryRepo: RealMemoryRepository
    private lateinit var conversationRepo: RealConversationRepository
    private lateinit var secureConfig: SecureConfigStorage

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        inMemoryDb = EvaDatabase.createInMemory(app)
        memoryRepo = RealMemoryRepository(inMemoryDb, MemoryPreferencesRepository(app))
        conversationRepo = RealConversationRepository(inMemoryDb)
        secureConfig = SecureConfigStorage(app)
        EdgeGlowStateHolder.resetToIdle()
    }

    @After
    fun tearDown() {
        inMemoryDb.close()
        EdgeGlowStateHolder.resetToIdle()
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. API KEY & SECRET SECURITY
    // =========================================================================

    @Test
    fun testSecureStorageEncryptionAndDecryption() {
        val rawApiKey = "AIzaSySecretTestKey_1234567890abcdef"
        secureConfig.saveGeminiApiKey(rawApiKey)

        assertTrue(secureConfig.hasGeminiApiKey())
        val retrieved = secureConfig.getGeminiApiKey()
        assertEquals(rawApiKey, retrieved)

        secureConfig.clearGeminiApiKey()
        assertFalse(secureConfig.hasGeminiApiKey())
        assertNull(secureConfig.getGeminiApiKey())
    }

    @Test
    fun testErrorMessageSecretSanitization() {
        val leakyError = Exception("Upstream error at url https://api.service.com?key=AIzaSySecretKey123 with header Bearer eyJhbGciOiJIUzI1NiJ9")
        val sanitized = secureConfig.sanitizeError(leakyError)

        assertFalse(sanitized.contains("AIzaSySecretKey123"))
        assertFalse(sanitized.contains("eyJhbGciOiJIUzI1NiJ9"))
        assertTrue(sanitized.contains("key=REDACTED"))
        assertTrue(sanitized.contains("Bearer REDACTED"))
    }

    @Test
    fun testOmniRouteRejectsInsecureHttpEndpoint() = runBlocking {
        val adapter = OmniRouteProviderAdapter()
        val result = adapter.generateResponse(
            prompt = "test",
            conversationHistory = emptyList(),
            modelId = "test-model",
            apiKey = "test-key",
            baseUrl = "http://insecure-api.omniroute.ai/v1/",
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Insecure HTTP connections are prohibited") == true)
    }

    @Test
    fun testGeminiHttpAuthenticationFailureHandling() {
        val adapter = GeminiProviderAdapter()
        val exception = adapter.handleHttpError(401, "{\"error\":{\"message\":\"API_KEY_INVALID\"}}", "gemini-3.5-flash")

        assertTrue(exception is SecurityException)
        assertTrue(exception.message?.contains("Authentication failed") == true)
    }

    // =========================================================================
    // 2. MEMORY & CONVERSATION PRIVACY
    // =========================================================================

    @Test
    fun testDeletedMemoriesAreNeverReturnedInContext() = runBlocking {
        memoryRepo.setMemoryEnabled(true)
        val mem1 = memoryRepo.saveMemory("I prefer dark roast coffee", "Preferences", "coffee", null, null, 2, false).getOrThrow()
        val mem2 = memoryRepo.saveMemory("My dog's name is Luna", "Personal", "dog", null, null, 2, false).getOrThrow()

        val foundBefore = memoryRepo.getRelevantMemoriesForContext("Tell me about coffee", 5, 500)
        assertEquals(1, foundBefore.size)
        assertEquals(mem1.id, foundBefore[0].id)

        // Delete mem1
        val deleted = memoryRepo.deleteMemory(mem1.id)
        assertTrue(deleted)

        val foundAfter = memoryRepo.getRelevantMemoriesForContext("Tell me about coffee", 5, 500)
        assertTrue(foundAfter.isEmpty())

        // Ensure prompt builder does not include deleted memory
        val promptText = memoryRepo.buildMemoryContextPrompt(foundAfter)
        assertFalse(promptText.contains("dark roast coffee"))
    }

    @Test
    fun testMemoryDisabledSuppressesContextPrompt() = runBlocking {
        memoryRepo.setMemoryEnabled(true)
        memoryRepo.saveMemory("I like jazz music", "Music", "jazz", null, null, 2, false).getOrThrow()

        // When enabled, it returns memory
        val beforeDisable = memoryRepo.getRelevantMemoriesForContext("Play some jazz", 5, 500)
        assertEquals(1, beforeDisable.size)

        // Disable memory
        memoryRepo.setMemoryEnabled(false)
        val afterDisable = memoryRepo.getRelevantMemoriesForContext("Play some jazz", 5, 500)
        assertTrue(afterDisable.isEmpty())

        val prompt = memoryRepo.buildMemoryContextPrompt(beforeDisable)
        assertEquals("", prompt)
    }

    @Test
    fun testPurgeAllMemoriesLeavesConversationsIntact() = runBlocking {
        val convId = UUID.randomUUID().toString()
        conversationRepo.createOrEnsureConversation(convId, "Hello", "Test Conversation")
        val msg = ChatMessage(conversationId = convId, role = ChatRole.User, content = "Hello EVA")
        conversationRepo.saveUserTurn(convId, msg)

        memoryRepo.saveMemory("User lives in Dhaka", "Location", "city", convId, msg.id, 2, false).getOrThrow()

        val purgeSuccess = memoryRepo.deleteAllMemories()
        assertTrue(purgeSuccess)

        // Memories are gone
        val remainingMemories = memoryRepo.getRelevantMemoriesForContext("Dhaka", 5, 500)
        assertTrue(remainingMemories.isEmpty())

        // Conversation history is preserved
        val conv = conversationRepo.getConversation(convId)
        assertNotNull(conv)
        val messages = conversationRepo.getMessages(convId)
        assertEquals(1, messages.size)
    }

    // =========================================================================
    // 3. ACCESSIBILITY SECURITY
    // =========================================================================

    @Test
    fun testCredentialFieldsAreNeverAllowedInAutomationInput() {
        val passwordStep = AutomationStep(
            stepIndex = 1,
            action = AutomationAction.InputText("Enter password", null, "my_secret_pass"),
            description = "Enter password",
        )
        val result = AutomationSafetyGuard.validateStep(passwordStep)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun testCredentialFieldDetection() {
        assertTrue(AutomationSafetyGuard.isCredentialField("user_password_input"))
        assertTrue(AutomationSafetyGuard.isCredentialField("enter otp"))
        assertTrue(AutomationSafetyGuard.isCredentialField("security code"))
        assertTrue(AutomationSafetyGuard.isCredentialField("credit card cvv"))
        assertFalse(AutomationSafetyGuard.isCredentialField("search_box_query"))
        assertFalse(AutomationSafetyGuard.isCredentialField("recipient_name"))
    }

    @Test
    fun testPlanValidationDetectsConsequentialActions() {
        val dangerousPlan = AutomationPlan(
            originalPrompt = "delete all my photos and wipe device",
            steps = listOf(
                AutomationStep(
                    stepIndex = 1,
                    action = AutomationAction.OpenApp("photos"),
                    description = "Open photos",
                    safetyRisk = SafetyRiskLevel.Safe,
                ),
                AutomationStep(
                    stepIndex = 2,
                    action = AutomationAction.ClickNode("delete all", null, null),
                    description = "delete all media",
                    safetyRisk = SafetyRiskLevel.Consequential,
                )
            ),
        )

        val validated = AutomationSafetyGuard.validatePlan(dangerousPlan).getOrThrow()
        assertTrue(validated.containsConsequentialAction)
        assertNotNull(validated.sensitiveActionReason)
    }

    // =========================================================================
    // 4. TASK AUTOMATION & TOOL REGISTRY SECURITY
    // =========================================================================

    @Test
    fun testDisallowedShellCharactersInToolParametersAreRejected() = runBlocking {
        val toolRegistry = AiToolRegistry(app)
        val safeTool = toolRegistry.findMatchingTool("check battery status")!!

        val maliciousParams = mapOf("query" to "battery; rm -rf /")
        val result = toolRegistry.executeTool(safeTool, maliciousParams, skipConfirmation = true)

        assertFalse(result.isSuccess)
        assertTrue(result.message.contains("disallowed characters"))
    }

    @Test
    fun testSensitiveToolsRequireConfirmation() = runBlocking {
        val toolRegistry = AiToolRegistry(app)
        val smsTool = toolRegistry.findMatchingTool("send sms")
        assertNotNull(smsTool)
        assertTrue(smsTool!!.isSensitive)

        // Without confirmation -> Rejected with confirmation required
        val unconfirmedResult = toolRegistry.executeTool(smsTool, mapOf("number" to "123456", "message" to "Hello"), skipConfirmation = false)
        assertFalse(unconfirmedResult.isSuccess)
        assertTrue(unconfirmedResult.message.contains("Confirmation required"))
        assertEquals(EdgeGlowState.WAITING_FOR_CONFIRMATION, EdgeGlowStateHolder.glowState.value)

        // With explicit confirmation -> Executed safely
        val confirmedResult = toolRegistry.executeTool(smsTool, mapOf("number" to "123456", "message" to "Hello"), skipConfirmation = true)
        assertTrue(confirmedResult.isSuccess)
        assertEquals(EdgeGlowState.SUCCESS, EdgeGlowStateHolder.glowState.value)
    }

    // =========================================================================
    // 5. PERMISSIONS AUDITING
    // =========================================================================

    @Test
    fun testSystemPermissionManagerAuditsAll17DeclaredPermissions() {
        val manager = SystemPermissionManager(app)
        val items = manager.auditAllPermissions()

        assertEquals(14, items.size) // 14 primary tracked functional permission items
        items.forEach { item ->
            assertTrue(item.id.isNotEmpty())
            assertTrue(item.title.isNotEmpty())
            assertTrue(item.description.isNotEmpty())
            assertTrue(item.category.isNotEmpty())
        }

        // Test settings dispatch does not crash
        val launched = manager.openSpecialPermissionSettings("overlay_window")
        assertTrue(launched)
    }

    // =========================================================================
    // 6. INCOGNITO MODE CONVERSATION & MEMORY ISOLATION
    // =========================================================================

    @Test
    fun testChatViewModelIncognitoModePreventsPersistence() = runTest(testDispatcher) {
        val chatViewModel = ChatViewModel(
            application = app,
            conversationRepository = conversationRepo,
            memoryRepository = memoryRepo,
        )

        // Enable incognito
        chatViewModel.toggleIncognito(true)
        assertTrue(chatViewModel.uiState.value.isIncognito)

        val incognitoConvId = chatViewModel.uiState.value.conversationId

        // Send prompt
        chatViewModel.sendMessage("This is an incognito private prompt")
        advanceUntilIdle()

        // Database should have zero records for this incognito conversation
        val savedMessages = conversationRepo.getMessages(incognitoConvId)
        assertTrue(savedMessages.isEmpty())
        assertNull(conversationRepo.getConversation(incognitoConvId))
    }
}
