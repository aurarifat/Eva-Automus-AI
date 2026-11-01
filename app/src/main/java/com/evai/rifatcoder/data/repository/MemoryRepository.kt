package com.evai.rifatcoder.data.repository

import com.evai.rifatcoder.data.database.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing user Personal Memories.
 * Maintains strict separation between raw conversation transcripts and user facts.
 */
interface MemoryRepository {

    val isMemoryEnabledFlow: StateFlow<Boolean>

    fun isMemoryEnabled(): Boolean

    fun setMemoryEnabled(enabled: Boolean)

    fun getConfirmedMemoriesFlow(): Flow<List<MemoryEntity>>

    fun getSuggestedMemoriesFlow(): Flow<List<MemoryEntity>>

    fun searchMemoriesFlow(query: String, category: String?): Flow<List<MemoryEntity>>

    fun countConfirmedMemories(): Flow<Int>

    suspend fun getMemoryById(id: String): MemoryEntity?

    suspend fun saveMemory(
        content: String,
        category: String,
        key: String = "",
        sourceConvId: String? = null,
        sourceMsgId: String? = null,
        importance: Int = 1,
        isSuggested: Boolean = false,
    ): Result<MemoryEntity>

    suspend fun updateMemory(
        id: String,
        content: String,
        category: String,
        importance: Int = 1,
    ): Result<MemoryEntity>

    suspend fun deleteMemory(id: String): Boolean

    suspend fun deleteAllMemories(): Boolean

    suspend fun confirmSuggestion(
        id: String,
        editedContent: String? = null,
        editedCategory: String? = null,
    ): Boolean

    suspend fun dismissSuggestion(id: String): Boolean

    suspend fun getRelevantMemoriesForContext(
        prompt: String,
        maxCount: Int = 5,
        maxChars: Int = 500,
    ): List<MemoryEntity>

    fun buildMemoryContextPrompt(memories: List<MemoryEntity>): String
}
