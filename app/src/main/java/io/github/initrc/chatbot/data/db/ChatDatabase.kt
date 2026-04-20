package io.github.initrc.chatbot.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        const val DATABASE_NAME = "chat_history.db"
    }
}
