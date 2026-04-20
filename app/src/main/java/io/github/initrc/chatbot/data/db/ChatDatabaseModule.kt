package io.github.initrc.chatbot.data.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatDatabaseModule {
    @Provides
    @Singleton
    fun provideChatDatabase(
        @ApplicationContext context: Context,
    ): ChatDatabase {
        return Room.databaseBuilder(
            context,
            ChatDatabase::class.java,
            ChatDatabase.DATABASE_NAME,
        ).build()
    }

    @Provides
    fun provideChatDao(database: ChatDatabase): ChatDao {
        return database.chatDao()
    }
}
