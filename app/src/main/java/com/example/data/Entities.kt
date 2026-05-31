package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey val id: String,
    val peerUsername: String,
    val peerPublicKeyBase64: String,
    val ourPrivateKeyBase64: String,
    val ourPublicKeyBase64: String,
    val sharedSecretBase64: String?,
    val isHost: Boolean,
    val connectionIp: String,
    val connectionPort: Int,
    val connectionType: String, // "P2P_SERVER", "P2P_CLIENT", "VIRTUAL_PEER"
    val isConnected: Boolean = false,
    val unreadCount: Int = 0,
    val lastMessageText: String? = null,
    val lastMessageTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderName: String,
    val encryptedPayload: String, // AES-GCM Encrypted Base64 Payload
    val isIncoming: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isEphemeral: Boolean = false,
    val ephemeralDurationSeconds: Int = 0, // e.g., 10, 30, 60 seconds
    val decimatedAt: Long = 0L,           // System.currentTimeMillis() + ephemeralDurationSeconds * 1000
    val isDecimated: Boolean = false      // marked as wiped out, hides original and shows visual fallout
)
