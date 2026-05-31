package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.crypto.AetherCrypto
import kotlinx.coroutines.flow.Flow

class AetherRepository(context: Context, private val dao: AetherDao) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aether_prefs", Context.MODE_PRIVATE)

    // User profile settings
    fun getUsername(): String? = prefs.getString("username", null)
    
    fun saveUsername(username: String) {
        prefs.edit().putString("username", username).apply()
    }

    fun getIdentityPublicKey(): String? = prefs.getString("identity_pub_key", null)
    fun getIdentityPrivateKey(): String? = prefs.getString("identity_priv_key", null)

    fun hasIdentity(): Boolean = getUsername() != null && getIdentityPublicKey() != null

    fun generateAndSaveIdentity(username: String) {
        val keyPair = AetherCrypto.generateKeyPair()
        val pubBase64 = AetherCrypto.publicKeyToBase64(keyPair.public)
        val privBase64 = AetherCrypto.privateKeyToBase64(keyPair.private)
        
        prefs.edit()
            .putString("username", username)
            .putString("identity_pub_key", pubBase64)
            .putString("identity_priv_key", privBase64)
            .apply()
    }

    fun getFingerprint(): String {
        val pub = getIdentityPublicKey() ?: return "AE-MUTED"
        return AetherCrypto.generateFingerprint(pub)
    }

    // Room DB Interactions
    val allChats: Flow<List<Chat>> = dao.getAllChats()

    fun getMessagesForChat(chatId: String): Flow<List<Message>> = dao.getMessagesForChat(chatId)

    suspend fun getChatById(id: String): Chat? = dao.getChatById(id)

    suspend fun insertChat(chat: Chat) = dao.insertChat(chat)

    suspend fun updateChat(chat: Chat) = dao.updateChat(chat)

    suspend fun deleteChatById(id: String) = dao.deleteChatById(id)

    suspend fun insertMessage(message: Message) = dao.insertMessage(message)

    suspend fun markMessageAsDecimated(id: String) = dao.markMessageAsDecimated(id)

    suspend fun deleteMessage(id: String) = dao.deleteMessage(id)

    suspend fun clearDecimated() = dao.clearDecimated()

    suspend fun getActiveEphemeralMessages(): List<Message> = dao.getActiveEphemeralMessages()
}
