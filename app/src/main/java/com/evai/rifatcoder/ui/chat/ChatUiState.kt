package com.evai.rifatcoder.ui.chat

import com.evai.rifatcoder.domain.chat.ChatMessage
import java.util.UUID

data class ChatUiState(
    val conversationId: String = UUID.randomUUID().toString(),
    val conversationTitle: String = "EVA AI",
    val messages: List<ChatMessage> = emptyList(),
    val composerText: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val selectedModel: String = "Not Configured (Phase 5)",
    val showClearConfirmDialog: Boolean = false,
    val showInfoDialog: Boolean = false,
    val showVoiceNoticeDialog: Boolean = false,
    val isIncognito: Boolean = false,
    val suggestedPrompts: List<String> = listOf(
        "What capabilities are planned for EVA AI?",
        "Explain the system architecture",
        "How will four-side edge glow work?",
    ),
) {
    val isEmpty: Boolean
        get() = messages.isEmpty()

    val canSend: Boolean
        get() = composerText.isNotBlank() && !isSending
}
