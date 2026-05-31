package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AetherDao {
    @Query("SELECT * FROM chats ORDER BY lastMessageTime DESC")
    fun getAllChats(): Flow<List<Chat>>

    @Query("SELECT * FROM chats WHERE id = :id LIMIT 1")
    suspend fun getChatById(id: String): Chat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat)

    @Update
    suspend fun updateChat(chat: Chat)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteChatById(id: String)

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("UPDATE messages SET isDecimated = 1 WHERE id = :id")
    suspend fun markMessageAsDecimated(id: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM messages WHERE isDecimated = 1")
    suspend fun clearDecimated()

    @Query("SELECT * FROM messages WHERE isEphemeral = 1 AND isDecimated = 0")
    suspend fun getActiveEphemeralMessages(): List<Message>
}
