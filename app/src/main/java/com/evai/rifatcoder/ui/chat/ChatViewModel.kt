package com.evai.rifatcoder.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evai.rifatcoder.data.database.EvaDatabase
import com.evai.rifatcoder.data.repository.ConversationRepository
import com.evai.rifatcoder.data.repository.MemoryRepository
import com.evai.rifatcoder.data.repository.RealConversationRepository
import com.evai.rifatcoder.data.repository.RealMemoryRepository
import com.evai.rifatcoder.data.security.SecureConfigStorage
import com.evai.rifatcoder.domain.chat.AiChatEngine
import com.evai.rifatcoder.domain.chat.ChatContextBuilder
import com.evai.rifatcoder.domain.chat.ChatMessage
import com.evai.rifatcoder.domain.chat.ChatRole
import com.evai.rifatcoder.domain.chat.MessageStatus
import com.evai.rifatcoder.domain.chat.RealAiChatEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel @JvmOverloads constructor(
    application: Application,
    private val chatEngine: AiChatEngine = RealAiChatEngine(SecureConfigStorage(application)),
    private val conversationRepository: ConversationRepository = RealConversationRepository(
        EvaDatabase.getInstance(application)
    ),
    private val memoryRepository: MemoryRepository = RealMemoryRepository(
        EvaDatabase.getInstance(application),
        com.evai.rifatcoder.data.memory.MemoryPreferencesRepository(application)
    ),
    private val memoryExtractionEngine: com.evai.rifatcoder.domain.memory.MemoryExtractionEngine =
        com.evai.rifatcoder.domain.memory.MemoryExtractionEngine(memoryRepository, chatEngine),
    private val automationCoordinator: com.evai.rifatcoder.ui.automation.AutomationCoordinator =
        com.evai.rifatcoder.ui.automation.AutomationCoordinator(context = application),
    private val contextBuilder: ChatContextBuilder = ChatContextBuilder(),
    private val aiToolRegistry: com.evai.rifatcoder.domain.tool.AiToolRegistry =
        com.evai.rifatcoder.domain.tool.AiToolRegistry(application),
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            selectedModel = chatEngine.activeModelName,
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null

    fun refreshEngineState() {
        _uiState.update { it.copy(selectedModel = chatEngine.activeModelName) }
    }

    fun updateComposerText(text: String) {
        _uiState.update { it.copy(composerText = text) }
    }

    fun loadConversation(conversationId: String) {
        if (conversationId.isBlank()) return
        cancelActiveRequest()

        viewModelScope.launch {
            val entity = conversationRepository.getConversation(conversationId)
            if (entity != null) {
                _uiState.update { current ->
                    current.copy(
                        conversationId = entity.id,
                        conversationTitle = entity.title,
                        isIncognito = false,
                    )
                }
            }
        }

        viewModelScope.launch {
            conversationRepository.getMessagesFlow(conversationId).collect { domainMessages ->
                _uiState.update { it.copy(messages = domainMessages) }
            }
        }
    }

    fun toggleIncognito(enabled: Boolean) {
        cancelActiveRequest()
        _uiState.update { current ->
            if (current.isIncognito != enabled) {
                current.copy(
                    isIncognito = enabled,
                    // If turning ON incognito, start a pristine ephemeral session
                    conversationId = if (enabled) UUID.randomUUID().toString() else current.conversationId,
                    messages = if (enabled) emptyList() else current.messages,
                    errorMessage = null,
                )
            } else {
                current
            }
        }
    }

    fun sendMessage(promptOverride: String? = null) {
        val prompt = (promptOverride ?: _uiState.value.composerText).trim()
        if (prompt.isBlank() || _uiState.value.isSending) return

        val convId = _uiState.value.conversationId
        val isIncognito = _uiState.value.isIncognito

        val userMessage = ChatMessage(
            conversationId = convId,
            role = ChatRole.User,
            content = prompt,
            status = MessageStatus.Sent,
        )

        // Clear composer only after local state acceptance
        _uiState.update { current ->
            current.copy(
                messages = current.messages + userMessage,
                composerText = if (promptOverride == null) "" else current.composerText,
                isSending = true,
                errorMessage = null,
            )
        }

        // Persist user turn to database ONLY if not in Incognito mode
        if (!isIncognito) {
            viewModelScope.launch {
                conversationRepository.saveUserTurn(convId, userMessage)
            }
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            // Check AI Tool Registry functions (Phase 15: Sections A-I)
            val matchingTool = aiToolRegistry.findMatchingTool(prompt)
            if (matchingTool != null) {
                val toolResult = aiToolRegistry.executeTool(matchingTool)
                val assistantMsg = ChatMessage(
                    conversationId = convId,
                    role = ChatRole.Assistant,
                    content = toolResult.message,
                    status = if (toolResult.isSuccess) MessageStatus.Complete else MessageStatus.Failed,
                )

                if (!isIncognito) {
                    conversationRepository.saveAssistantTurn(convId, assistantMsg)
                }

                _uiState.update { current ->
                    if (current.conversationId == convId) {
                        current.copy(
                            messages = current.messages + assistantMsg,
                            isSending = false,
                        )
                    } else current
                }
                return@launch
            }

            // Check if user input is an explicit device automation command
            if (automationCoordinator.isAutomationCommand(prompt)) {
                automationCoordinator.submitCommand(prompt)
                val report = automationCoordinator.uiState.value.lastExecutionReport
                val replyText = report?.summary
                    ?: if (automationCoordinator.uiState.value.taskStatus == com.evai.rifatcoder.domain.automation.AutomationTaskStatus.WaitingForConfirmation) {
                        "This command requires confirmation: ${automationCoordinator.uiState.value.pendingConfirmationReason}. Please confirm in the Automation screen."
                    } else {
                        "Executing automation task for \"$prompt\"..."
                    }

                val assistantMsg = ChatMessage(
                    conversationId = convId,
                    role = ChatRole.Assistant,
                    content = replyText,
                    status = MessageStatus.Complete,
                )

                if (!isIncognito) {
                    conversationRepository.saveAssistantTurn(convId, assistantMsg)
                }

                _uiState.update { current ->
                    if (current.conversationId == convId) {
                        current.copy(
                            messages = current.messages + assistantMsg,
                            isSending = false,
                        )
                    } else current
                }
                return@launch
            }

            // Retrieve relevant personal memories if Personal Memory is enabled and NOT in Incognito
            val relevantMemories = if (!isIncognito && memoryRepository.isMemoryEnabled()) {
                memoryRepository.getRelevantMemoriesForContext(prompt)
            } else {
                emptyList()
            }

            // Build structured 3-layer context
            val preparedContext = contextBuilder.buildContext(
                conversationHistory = _uiState.value.messages,
                userPrompt = prompt,
                relevantMemories = relevantMemories,
                isIncognito = isIncognito,
            )

            val result = chatEngine.sendPrompt(
                conversationId = convId,
                history = preparedContext.boundedHistory,
                prompt = preparedContext.finalPrompt,
                memoryContext = null, // Embedded in structured finalPrompt
            )

            result.fold(
                onSuccess = { replyText ->
                    val assistantMsg = ChatMessage(
                        conversationId = convId,
                        role = ChatRole.Assistant,
                        content = replyText,
                        status = MessageStatus.Complete,
                    )

                    // Persist assistant turn to database ONLY if not in Incognito mode
                    if (!isIncognito) {
                        conversationRepository.saveAssistantTurn(convId, assistantMsg)
                    }

                    _uiState.update { current ->
                        if (current.conversationId == convId) {
                            current.copy(
                                messages = current.messages + assistantMsg,
                                isSending = false,
                            )
                        } else {
                            current
                        }
                    }

                    // Propose memory suggestion ONLY if not in Incognito mode and Memory is enabled
                    if (!isIncognito && memoryRepository.isMemoryEnabled()) {
                        memoryExtractionEngine.inspectAndProposeMemory(
                            userPrompt = prompt,
                            assistantResponse = replyText,
                            conversationId = convId,
                            messageId = assistantMsg.id,
                        )
                    }
                },
                onFailure = { error ->
                    val isConfigIssue = !chatEngine.isConfigured
                    val noticeMsg = ChatMessage(
                        conversationId = convId,
                        role = ChatRole.Assistant,
                        content = error.message ?: "Failed to generate AI response.",
                        status = if (isConfigIssue) MessageStatus.NotConfigured else MessageStatus.Failed,
                        errorMessage = error.localizedMessage,
                    )

                    if (!isIncognito) {
                        conversationRepository.saveAssistantTurn(convId, noticeMsg)
                    }

                    _uiState.update { current ->
                        if (current.conversationId == convId) {
                            current.copy(
                                messages = current.messages + noticeMsg,
                                isSending = false,
                            )
                        } else {
                            current
                        }
                    }
                }
            )
        }
    }

    fun cancelActiveRequest() {
        activeJob?.cancel()
        activeJob = null
        _uiState.update { it.copy(isSending = false) }
    }

    fun retryLastMessage() {
        val lastUserMsg = _uiState.value.messages.lastOrNull { it.role == ChatRole.User }
        if (lastUserMsg != null) {
            sendMessage(lastUserMsg.content)
        }
    }

    fun retryMessage(messageId: String? = null) {
        if (messageId != null) {
            val targetMsg = _uiState.value.messages.find { it.id == messageId }
            if (targetMsg != null) {
                val promptToResend = if (targetMsg.role == ChatRole.User) {
                    targetMsg.content
                } else {
                    _uiState.value.messages.takeWhile { it.id != messageId }
                        .lastOrNull { it.role == ChatRole.User }?.content
                }
                if (!promptToResend.isNullOrBlank()) {
                    sendMessage(promptToResend)
                    return
                }
            }
        }
        retryLastMessage()
    }

    fun startNewChat() {
        cancelActiveRequest()
        _uiState.update {
            ChatUiState(
                conversationId = UUID.randomUUID().toString(),
                conversationTitle = "EVA AI",
                selectedModel = chatEngine.activeModelName,
                isIncognito = it.isIncognito,
            )
        }
    }

    fun requestClearConversation() {
        _uiState.update { it.copy(showClearConfirmDialog = true) }
    }

    fun dismissClearDialog() {
        _uiState.update { it.copy(showClearConfirmDialog = false) }
    }

    fun confirmClearConversation() {
        val convId = _uiState.value.conversationId
        val isIncognito = _uiState.value.isIncognito

        cancelActiveRequest()

        if (!isIncognito) {
            viewModelScope.launch {
                conversationRepository.deleteConversation(convId)
            }
        }

        _uiState.update {
            it.copy(
                messages = emptyList(),
                showClearConfirmDialog = false,
                errorMessage = null,
            )
        }
    }

    fun showInfoDialog() {
        _uiState.update { it.copy(showInfoDialog = true) }
    }

    fun dismissInfoDialog() {
        _uiState.update { it.copy(showInfoDialog = false) }
    }

    fun showVoiceNotice() {
        _uiState.update { it.copy(showVoiceNoticeDialog = true) }
    }

    fun dismissVoiceNotice() {
        _uiState.update { it.copy(showVoiceNoticeDialog = false) }
    }
}
