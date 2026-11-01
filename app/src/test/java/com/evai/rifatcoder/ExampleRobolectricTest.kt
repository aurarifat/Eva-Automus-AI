package com.evai.rifatcoder

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.evai.rifatcoder.data.security.AiProviderType
import com.evai.rifatcoder.data.security.ProviderConnectionStatus
import com.evai.rifatcoder.data.security.SecureConfigStorage
import com.evai.rifatcoder.domain.chat.ChatRole
import com.evai.rifatcoder.ui.chat.ChatViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verify app name resource matches EVA AI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("EVA AI", appName)
    }

    @Test
    fun `verify developer name resource matches Aura RIFAT`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val developer = context.getString(R.string.app_developer)
        assertEquals("Aura RIFAT", developer)
    }

    @Test
    fun `verify tagline resource matches Your assistant, your control`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tagline = context.getString(R.string.app_tagline)
        assertEquals("Your assistant, your control.", tagline)
    }

    @Test
    fun `verify all five bottom navigation string resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Home", context.getString(R.string.nav_home))
        assertEquals("Chat", context.getString(R.string.nav_chat))
        assertEquals("Voice", context.getString(R.string.nav_voice))
        assertEquals("Memory", context.getString(R.string.nav_memory))
        assertEquals("Settings", context.getString(R.string.nav_settings))
    }

    @Test
    fun `verify Phase 4 Chat string resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("How can I help you?", context.getString(R.string.chat_empty_title))
        assertEquals("Ask a question to start a conversation.", context.getString(R.string.chat_empty_subtitle))
        assertEquals("New Chat", context.getString(R.string.chat_new_conversation))
        assertEquals("Clear Conversation", context.getString(R.string.chat_clear_conversation))
        assertEquals("Send Message", context.getString(R.string.chat_cd_send))
    }

    @Test
    fun `verify Phase 5 AI Connection string resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Configure AI", context.getString(R.string.btn_configure_ai))
        assertEquals("Save API Key", context.getString(R.string.config_save_key))
        assertEquals("Test Connection", context.getString(R.string.config_test_connection))
        assertEquals("Remove API Key", context.getString(R.string.config_remove_key))
        assertEquals("Active Model", context.getString(R.string.config_model_id_label))
    }

    @Test
    fun `verify SecureConfigStorage saves and isolates credentials`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = SecureConfigStorage(context)

        // Default active provider is GEMINI
        assertEquals(AiProviderType.GEMINI, storage.getActiveProvider())

        // Save active provider as OMNIROUTE and verify
        storage.setActiveProvider(AiProviderType.OMNIROUTE)
        assertEquals(AiProviderType.OMNIROUTE, storage.getActiveProvider())

        // Switch back to GEMINI
        storage.setActiveProvider(AiProviderType.GEMINI)
        assertEquals(AiProviderType.GEMINI, storage.getActiveProvider())

        // Save Gemini key
        val testGeminiKey = "test-gemini-key-12345"
        storage.saveGeminiApiKey(testGeminiKey)
        assertTrue(storage.hasGeminiApiKey())
        assertTrue(storage.hasApiKey(AiProviderType.GEMINI))
        assertEquals(testGeminiKey, storage.getGeminiApiKey())

        // Verify OmniRoute key is still empty (credential isolation)
        assertFalse(storage.hasOmniRouteApiKey())
        assertFalse(storage.hasApiKey(AiProviderType.OMNIROUTE))
        assertNull(storage.getOmniRouteApiKey())

        // Save OmniRoute key
        val testOmniKey = "sk-test-omniroute-key-999"
        storage.saveOmniRouteApiKey(testOmniKey)
        assertTrue(storage.hasOmniRouteApiKey())
        assertEquals(testOmniKey, storage.getOmniRouteApiKey())

        // Verify Gemini key is unaffected
        assertEquals(testGeminiKey, storage.getGeminiApiKey())

        // Test clear key
        storage.clearGeminiApiKey()
        assertFalse(storage.hasGeminiApiKey())
        assertNull(storage.getGeminiApiKey())
        assertEquals(ProviderConnectionStatus.NOT_CONFIGURED, storage.getProviderStatus(AiProviderType.GEMINI))

        // OmniRoute key remains
        assertTrue(storage.hasOmniRouteApiKey())
        assertEquals(testOmniKey, storage.getOmniRouteApiKey())
        storage.clearOmniRouteApiKey()
        assertFalse(storage.hasOmniRouteApiKey())
    }

    @Test
    fun `verify SecureConfigStorage status transitions and model persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = SecureConfigStorage(context)

        // Model ID persistence
        assertEquals("gemini-3.5-flash", storage.getGeminiModelId())
        storage.saveGeminiModelId("gemini-1.5-pro")
        assertEquals("gemini-1.5-pro", storage.getGeminiModelId())

        assertEquals("omni-default", storage.getOmniRouteModelId())
        storage.saveOmniRouteModelId("omni-pro")
        assertEquals("omni-pro", storage.getOmniRouteModelId())

        // Status transitions
        storage.setProviderStatus(AiProviderType.GEMINI, ProviderConnectionStatus.TESTING)
        assertEquals(ProviderConnectionStatus.TESTING, storage.getProviderStatus(AiProviderType.GEMINI))

        storage.setProviderStatus(AiProviderType.GEMINI, ProviderConnectionStatus.CONNECTED)
        assertEquals(ProviderConnectionStatus.CONNECTED, storage.getProviderStatus(AiProviderType.GEMINI))

        storage.setProviderStatus(AiProviderType.GEMINI, ProviderConnectionStatus.CONNECTION_FAILED)
        assertEquals(ProviderConnectionStatus.CONNECTION_FAILED, storage.getProviderStatus(AiProviderType.GEMINI))
    }

    @Test
    fun `verify ChatViewModel composer and submission in Robolectric`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ChatViewModel(app)

        assertFalse(viewModel.uiState.value.canSend)
        viewModel.updateComposerText("Hello from Robolectric test")
        assertTrue(viewModel.uiState.value.canSend)
        assertEquals("Hello from Robolectric test", viewModel.uiState.value.composerText)

        viewModel.sendMessage()
        assertEquals("", viewModel.uiState.value.composerText)
        assertFalse(viewModel.uiState.value.isEmpty)
        val msg = viewModel.uiState.value.messages.first()
        assertEquals(ChatRole.User, msg.role)
        assertEquals("Hello from Robolectric test", msg.content)

        // Cancel active request
        viewModel.cancelActiveRequest()
        assertFalse(viewModel.uiState.value.isSending)

        // New chat resets conversation
        val oldId = viewModel.uiState.value.conversationId
        viewModel.startNewChat()
        assertTrue(viewModel.uiState.value.isEmpty)
        assertFalse(oldId == viewModel.uiState.value.conversationId)
    }

    @Test
    fun `verify ChatViewModel prevents blank message submission`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ChatViewModel(app)
        viewModel.updateComposerText("     ")
        assertFalse(viewModel.uiState.value.canSend)
        viewModel.sendMessage()
        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `verify MainActivity creates and starts without crashing`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
        controller.create().start().resume()
        val activity = controller.get()
        assertNotNull(activity)
        val homeVm = androidx.lifecycle.ViewModelProvider(activity)[com.evai.rifatcoder.ui.home.HomeViewModel::class.java]
        assertNotNull(homeVm)
        controller.pause().stop().destroy()
    }
}
