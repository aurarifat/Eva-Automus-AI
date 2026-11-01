package com.evai.rifatcoder.ui.memory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evai.rifatcoder.data.database.EvaDatabase
import com.evai.rifatcoder.data.database.entity.MemoryEntity
import com.evai.rifatcoder.data.memory.MemoryPreferencesRepository
import com.evai.rifatcoder.data.repository.MemoryRepository
import com.evai.rifatcoder.data.repository.RealMemoryRepository
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

data class MemoryUiState(
    val isMemoryEnabled: Boolean = true,
    val savedCount: Int = 0,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val confirmedMemories: List<MemoryEntity> = emptyList(),
    val suggestedMemories: List<MemoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isAddDialogOpen: Boolean = false,
    val editingMemory: MemoryEntity? = null,
    val deletingMemory: MemoryEntity? = null,
    val showClearAllDialog: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryViewModel @JvmOverloads constructor(
    application: Application,
    private val memoryRepository: MemoryRepository = RealMemoryRepository(
        EvaDatabase.getInstance(application),
        MemoryPreferencesRepository(application)
    ),
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _isAddDialogOpen = MutableStateFlow(false)
    private val _editingMemory = MutableStateFlow<MemoryEntity?>(null)
    private val _deletingMemory = MutableStateFlow<MemoryEntity?>(null)
    private val _showClearAllDialog = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _successMessage = MutableStateFlow<String?>(null)

    // Reactive filtered confirmed memories
    private val filteredMemoriesFlow = combine(_searchQuery, _selectedCategory) { query, cat ->
        Pair(query, cat)
    }.flatMapLatest { (query, cat) ->
        memoryRepository.searchMemoriesFlow(query, cat)
    }

    val uiState: StateFlow<MemoryUiState> = combine(
        memoryRepository.isMemoryEnabledFlow,
        memoryRepository.countConfirmedMemories(),
        filteredMemoriesFlow,
        memoryRepository.getSuggestedMemoriesFlow(),
    ) { enabled, count, confirmed, suggested ->
        MemoryUiState(
            isMemoryEnabled = enabled,
            savedCount = count,
            searchQuery = _searchQuery.value,
            selectedCategory = _selectedCategory.value,
            confirmedMemories = confirmed,
            suggestedMemories = suggested,
            isAddDialogOpen = _isAddDialogOpen.value,
            editingMemory = _editingMemory.value,
            deletingMemory = _deletingMemory.value,
            showClearAllDialog = _showClearAllDialog.value,
            errorMessage = _errorMessage.value,
            successMessage = _successMessage.value,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MemoryUiState(
            isMemoryEnabled = memoryRepository.isMemoryEnabled(),
        ),
    )

    fun toggleMemoryEnabled(enabled: Boolean) {
        memoryRepository.setMemoryEnabled(enabled)
        _successMessage.value = if (enabled) {
            "Personal Memory activated. Saved facts will assist your conversations."
        } else {
            "Personal Memory paused. Existing facts are preserved but will not be used."
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = if (category == _selectedCategory.value) null else category
    }

    fun openAddDialog() {
        _errorMessage.value = null
        _isAddDialogOpen.value = true
    }

    fun closeAddDialog() {
        _isAddDialogOpen.value = false
    }

    fun openEditDialog(memory: MemoryEntity) {
        _errorMessage.value = null
        _editingMemory.value = memory
    }

    fun closeEditDialog() {
        _editingMemory.value = null
    }

    fun openDeleteDialog(memory: MemoryEntity) {
        _deletingMemory.value = memory
    }

    fun closeDeleteDialog() {
        _deletingMemory.value = null
    }

    fun openClearAllDialog() {
        _showClearAllDialog.value = true
    }

    fun closeClearAllDialog() {
        _showClearAllDialog.value = false
    }

    fun saveMemory(content: String, category: String, importance: Int = 1) {
        viewModelScope.launch {
            val result = memoryRepository.saveMemory(
                content = content,
                category = category,
                key = content.take(24).trimEnd(),
                importance = importance,
                isSuggested = false,
            )

            result.fold(
                onSuccess = {
                    _isAddDialogOpen.value = false
                    _errorMessage.value = null
                    _successMessage.value = "Memory saved successfully."
                },
                onFailure = { error ->
                    _errorMessage.value = error.localizedMessage ?: "Failed to save memory."
                }
            )
        }
    }

    fun updateMemory(id: String, content: String, category: String, importance: Int = 1) {
        viewModelScope.launch {
            val result = memoryRepository.updateMemory(
                id = id,
                content = content,
                category = category,
                importance = importance,
            )

            result.fold(
                onSuccess = {
                    _editingMemory.value = null
                    _errorMessage.value = null
                    _successMessage.value = "Memory updated."
                },
                onFailure = { error ->
                    _errorMessage.value = error.localizedMessage ?: "Failed to update memory."
                }
            )
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            val success = memoryRepository.deleteMemory(id)
            _deletingMemory.value = null
            if (success) {
                _successMessage.value = "Memory deleted."
            } else {
                _errorMessage.value = "Could not delete memory."
            }
        }
    }

    fun deleteAllMemories() {
        viewModelScope.launch {
            val success = memoryRepository.deleteAllMemories()
            _showClearAllDialog.value = false
            if (success) {
                _successMessage.value = "All personal memories cleared. Conversation history preserved."
            } else {
                _errorMessage.value = "Could not clear memories."
            }
        }
    }

    fun confirmSuggestion(id: String, editedContent: String? = null, editedCategory: String? = null) {
        viewModelScope.launch {
            val success = memoryRepository.confirmSuggestion(id, editedContent, editedCategory)
            if (success) {
                _successMessage.value = "Suggestion confirmed and saved to memory bank."
            } else {
                _errorMessage.value = "Failed to confirm suggestion."
            }
        }
    }

    fun dismissSuggestion(id: String) {
        viewModelScope.launch {
            memoryRepository.dismissSuggestion(id)
            _successMessage.value = "Suggestion dismissed."
        }
    }

    fun dismissMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
