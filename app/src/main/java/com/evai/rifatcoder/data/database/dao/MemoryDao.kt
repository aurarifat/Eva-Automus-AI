package com.evai.rifatcoder.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.evai.rifatcoder.data.database.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE isConfirmed = 1 AND isSuggested = 0 ORDER BY updatedAt DESC")
    fun getConfirmedMemoriesFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE isSuggested = 1 ORDER BY updatedAt DESC")
    fun getSuggestedMemoriesFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE isConfirmed = 1 AND isSuggested = 0 AND (value LIKE '%' || :query || '%' OR key LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    fun searchConfirmedMemoriesFlow(query: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE isConfirmed = 1 AND isSuggested = 0 AND category = :category AND (value LIKE '%' || :query || '%' OR key LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    fun searchConfirmedMemoriesWithCategoryFlow(query: String, category: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE isConfirmed = 1 AND isSuggested = 0 AND category = :category ORDER BY updatedAt DESC")
    fun getConfirmedMemoriesByCategoryFlow(category: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE isConfirmed = 1 AND isSuggested = 0 ORDER BY updatedAt DESC")
    suspend fun getConfirmedMemories(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    suspend fun getMemoryById(id: String): MemoryEntity?

    @Query("SELECT * FROM memories WHERE LOWER(value) = LOWER(:content) AND isConfirmed = 1 LIMIT 1")
    suspend fun findDuplicate(content: String): MemoryEntity?

    @Query("SELECT COUNT(*) FROM memories WHERE isConfirmed = 1 AND isSuggested = 0")
    fun countConfirmedMemories(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String): Int

    @Query("DELETE FROM memories")
    suspend fun deleteAllMemories(): Int

    @Query("UPDATE memories SET isConfirmed = 1, isSuggested = 0, value = :content, category = :category, updatedAt = :updatedAt WHERE id = :id")
    suspend fun confirmMemory(id: String, content: String, category: String, updatedAt: Long): Int

    @Query("DELETE FROM memories WHERE id = :id AND isSuggested = 1")
    suspend fun dismissSuggestion(id: String): Int
}
