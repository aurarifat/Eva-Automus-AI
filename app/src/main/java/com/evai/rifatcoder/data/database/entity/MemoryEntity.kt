package com.evai.rifatcoder.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Personal Memory entity representing discrete, user-controlled facts, preferences,
 * ongoing projects, or tools. Kept strictly separate from raw conversation transcripts.
 */
@Entity(
    tableName = "memories",
    indices = [
        Index(value = ["category"]),
        Index(value = ["updatedAt"]),
        Index(value = ["isConfirmed"]),
        Index(value = ["isSuggested"]),
    ],
)
data class MemoryEntity(
    @PrimaryKey
    val id: String,
    val category: String,
    val key: String = "",
    val value: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sourceConversationId: String? = null,
    val sourceMessageId: String? = null,
    val isConfirmed: Boolean = true,
    val importance: Int = 1,
    val isSuggested: Boolean = false,
) {
    /**
     * Semantic alias for the memory's fact content.
     */
    val content: String
        get() = value
}
