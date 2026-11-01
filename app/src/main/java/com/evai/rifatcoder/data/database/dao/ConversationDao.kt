package com.evai.rifatcoder.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.evai.rifatcoder.data.database.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversationsFlow(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentConversationsFlow(limit: Int): Flow<List<ConversationEntity>>

    @Query(
        """
        SELECT * FROM conversations 
        WHERE title LIKE '%' || :query || '%' 
           OR lastMessagePreview LIKE '%' || :query || '%' 
        ORDER BY updatedAt DESC
        """
    )
    fun searchConversationsFlow(query: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query(
        """
        UPDATE conversations 
        SET title = :newTitle, isTitleManuallyEdited = :isManual, updatedAt = :updatedAt 
        WHERE id = :id
        """
    )
    suspend fun updateTitle(
        id: String,
        newTitle: String,
        isManual: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE conversations 
        SET lastMessagePreview = :preview, updatedAt = :updatedAt 
        WHERE id = :id
        """
    )
    suspend fun updateLastMessagePreview(
        id: String,
        preview: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: String): Int

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations(): Int

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getConversationCount(): Int
}
