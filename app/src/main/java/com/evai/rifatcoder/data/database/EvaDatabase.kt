package com.evai.rifatcoder.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.evai.rifatcoder.data.database.dao.ConversationDao
import com.evai.rifatcoder.data.database.dao.MemoryDao
import com.evai.rifatcoder.data.database.dao.MessageDao
import com.evai.rifatcoder.data.database.entity.ConversationEntity
import com.evai.rifatcoder.data.database.entity.MemoryEntity
import com.evai.rifatcoder.data.database.entity.MessageEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MemoryEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class EvaDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: EvaDatabase? = null

        /**
         * Safe, non-destructive migration from Version 1 to Version 2.
         * Adds source links, confirmation status, importance, and suggestion status to memories table.
         * Preserves all existing conversations, messages, and prior memories without data loss.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE memories ADD COLUMN sourceConversationId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE memories ADD COLUMN sourceMessageId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE memories ADD COLUMN isConfirmed INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE memories ADD COLUMN importance INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE memories ADD COLUMN isSuggested INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_memories_isConfirmed ON memories(isConfirmed)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_memories_isSuggested ON memories(isSuggested)")
            }
        }

        fun getInstance(context: Context): EvaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EvaDatabase::class.java,
                    "eva_ai.db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
        }

        /**
         * In-memory database factory for Robolectric and local unit tests.
         */
        fun createInMemory(context: Context): EvaDatabase {
            return Room.inMemoryDatabaseBuilder(context, EvaDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }
    }
}
