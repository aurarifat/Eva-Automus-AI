package com.evai.rifatcoder.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evai.rifatcoder.data.database.EvaDatabase
import com.evai.rifatcoder.data.database.entity.ConversationEntity
import com.evai.rifatcoder.data.repository.ConversationRepository
import com.evai.rifatcoder.data.repository.RealConversationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val searchQuery: String = "",
    val conversations: List<ConversationEntity> = emptyList(),
    val isLoading: Boolean = false,
    val renameTarget: ConversationEntity? = null,
    val renameInput: String = "",
    val deleteTarget: ConversationEntity? = null,
    val showClearAllConfirm: Boolean = false,
)

class ConversationHistoryViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ConversationRepository = RealConversationRepository(
        EvaDatabase.getInstance(application)
    ),
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    private val _dialogState = MutableStateFlow(
        DialogState()
    )

    private data class DialogState(
        val renameTarget: ConversationEntity? = null,
        val renameInput: String = "",
        val deleteTarget: ConversationEntity? = null,
        val showClearAllConfirm: Boolean = false,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = combine(
        _searchQuery,
        _searchQuery.flatMapLatest { query ->
            repository.searchConversations(query)
        },
        _dialogState,
    ) { query, conversations, dialogs ->
        HistoryUiState(
            searchQuery = query,
            conversations = conversations,
            isLoading = false,
            renameTarget = dialogs.renameTarget,
            renameInput = dialogs.renameInput,
            deleteTarget = dialogs.deleteTarget,
            showClearAllConfirm = dialogs.showClearAllConfirm,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(isLoading = true),
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onClearSearch() {
        _searchQuery.value = ""
    }

    fun onStartRename(conversation: ConversationEntity) {
        _dialogState.update {
            it.copy(
                renameTarget = conversation,
                renameInput = conversation.title,
            )
        }
    }

    fun onRenameInputChange(text: String) {
        _dialogState.update { it.copy(renameInput = text) }
    }

    fun onConfirmRename() {
        val target = _dialogState.value.renameTarget ?: return
        val newTitle = _dialogState.value.renameInput.trim()
        if (newTitle.isNotBlank()) {
            viewModelScope.launch {
                repository.renameConversation(target.id, newTitle)
            }
        }
        onDismissRename()
    }

    fun onDismissRename() {
        _dialogState.update {
            it.copy(renameTarget = null, renameInput = "")
        }
    }

    fun onStartDelete(conversation: ConversationEntity) {
        _dialogState.update { it.copy(deleteTarget = conversation) }
    }

    fun onConfirmDelete() {
        val target = _dialogState.value.deleteTarget ?: return
        viewModelScope.launch {
            repository.deleteConversation(target.id)
        }
        onDismissDelete()
    }

    fun onDismissDelete() {
        _dialogState.update { it.copy(deleteTarget = null) }
    }

    fun onRequestClearAll() {
        _dialogState.update { it.copy(showClearAllConfirm = true) }
    }

    fun onConfirmClearAll() {
        viewModelScope.launch {
            repository.clearAllConversations()
        }
        onDismissClearAll()
    }

    fun onDismissClearAll() {
        _dialogState.update { it.copy(showClearAllConfirm = false) }
    }
}
