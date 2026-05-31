package com.example.ui

import android.app.Application
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.AetherCrypto
import com.example.data.*
import com.example.network.AetherP2PEngine
import com.example.network.GeminiClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

class AetherViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AetherRepository(application, database.aetherDao())

    // UI state flows
    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _fingerprint = MutableStateFlow("AE-MUTED")
    val fingerprint: StateFlow<String> = _fingerprint.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Onboarding)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    private val _p2pDiagnosticLogs = MutableStateFlow<List<String>>(emptyList())
    val p2pDiagnosticLogs: StateFlow<List<String>> = _p2pDiagnosticLogs.asStateFlow()

    // Chats & Messages Reactivity
    val allChats: StateFlow<List<Chat>> = repository.allChats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMessages: StateFlow<List<Message>> = _selectedChatId
        .flatMapLatest { chatId ->
            if (chatId == null) flowOf(emptyList())
            else repository.getMessagesForChat(chatId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active disappearing timer duration in active conversation (default: 0 = off, 15 = 15s, etc.)
    private val _activeEphemeralDuration = MutableStateFlow(0)
    val activeEphemeralDuration: StateFlow<Int> = _activeEphemeralDuration.asStateFlow()

    init {
        // Load Profile
        _username.value = repository.getUsername()
        _fingerprint.value = repository.getFingerprint()
        
        if (repository.hasIdentity()) {
            _currentScreen.value = Screen.ChatsList
        }

        // Start local P2P Server socket
        startP2PServer()

        // Listen for internal P2P Connection events
        subscribeToP2PEvents()

        // Start ephemeral purge daemon
        startEphemeralPurgeDaemon()
    }

    // Onboarding Registration
    fun registerUser(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            repository.generateAndSaveIdentity(trimmed)
            _username.value = trimmed
            _fingerprint.value = repository.getFingerprint()
            _currentScreen.value = Screen.ChatsList
            addDiagnosticLog("Identity signature generated. Fingerprint: ${_fingerprint.value}")
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.ConversationByIP) {
            _selectedChatId.value = screen.chatId
            _activeEphemeralDuration.value = 0
            // Reset unread count
            viewModelScope.launch {
                repository.getChatById(screen.chatId)?.let { chat ->
                    repository.updateChat(chat.copy(unreadCount = 0))
                }
            }
        } else if (screen !is Screen.ConversationByIP) {
            _selectedChatId.value = null
        }
    }

    // Toggle Ephemeral Self-destruct timers
    fun setEphemeralDuration(seconds: Int) {
        _activeEphemeralDuration.value = seconds
        addDiagnosticLog("Ephemeral window configured to ${seconds}s")
    }

    // Start Raw TCP listener
    private fun startP2PServer() {
        viewModelScope.launch {
            AetherP2PEngine.startServer(
                getApplication(),
                port = 8999,
                repository = repository,
                scope = viewModelScope
            )
        }
    }

    // Hook up socket events to Database state and UI
    private fun subscribeToP2PEvents() {
        viewModelScope.launch {
            AetherP2PEngine.events.collect { event ->
                when (event) {
                    is AetherP2PEngine.P2PEvent.ConnectionEstablished -> {
                        addDiagnosticLog("Handshake accepted with ${event.peerUsername}. ECDH established.")
                    }
                    is AetherP2PEngine.P2PEvent.ConnectionClosed -> {
                        addDiagnosticLog("Quantum tunnel to $event collapsed.")
                    }
                    is AetherP2PEngine.P2PEvent.MessageReceived -> {
                        handleIncomingP2PMessage(
                            msgId = event.messageId,
                            chatId = event.chatId,
                            senderName = event.senderName,
                            encryptedPayload = event.encryptedPayload,
                            isEphemeral = event.isEphemeral,
                            durationSeconds = event.ephemeralDurationSeconds
                        )
                    }
                    is AetherP2PEngine.P2PEvent.Error -> {
                        addDiagnosticLog("Quantum Core Error: ${event.text}")
                    }
                    is AetherP2PEngine.P2PEvent.Diagnostic -> {
                        addDiagnosticLog(event.log)
                    }
                }
            }
        }
    }

    // Process decrypted and securely persist incoming message
    private suspend fun handleIncomingP2PMessage(
        msgId: String,
        chatId: String,
        senderName: String,
        encryptedPayload: String,
        isEphemeral: Boolean,
        durationSeconds: Int
    ) {
        val chat = repository.getChatById(chatId) ?: return
        val sharedSecretKey = chat.sharedSecretBase64?.let { Base64.decode(it, Base64.NO_WRAP) }

        val decryptedText = if (sharedSecretKey != null) {
            AetherCrypto.decrypt(encryptedPayload, sharedSecretKey)
        } else {
            "[KEY_AGREEMENT_MISSING]"
        }

        val decimatedTime = if (isEphemeral) {
            System.currentTimeMillis() + (durationSeconds * 1000)
        } else {
            0L
        }

        val message = Message(
            id = msgId,
            chatId = chatId,
            senderName = senderName,
            encryptedPayload = encryptedPayload,
            isIncoming = true,
            timestamp = System.currentTimeMillis(),
            isEphemeral = isEphemeral,
            ephemeralDurationSeconds = durationSeconds,
            decimatedAt = decimatedTime
        )

        repository.insertMessage(message)

        // Update chat summary
        repository.updateChat(chat.copy(
            lastMessageText = if (isEphemeral) "🔒 Ephemeral transmission received" else decryptedText,
            lastMessageTime = System.currentTimeMillis(),
            unreadCount = if (_selectedChatId.value != chatId) chat.unreadCount + 1 else 0
        ))
    }

    // Send Outgoing E2EE message (Real or Simulated Bot)
    fun sendMessage(text: String) {
        val chatId = _selectedChatId.value ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val chat = repository.getChatById(chatId) ?: return@launch
            val isVirtual = chat.connectionType == "VIRTUAL_PEER"

            val sharedSecretKey = chat.sharedSecretBase64?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return@launch
            val messageId = UUID.randomUUID().toString()

            // Encrypt plaintext with derived AES secret key
            val encrypted = AetherCrypto.encrypt(trimmed, sharedSecretKey)

            val isTimerOn = _activeEphemeralDuration.value > 0
            val seconds = _activeEphemeralDuration.value
            val decimatedTime = if (isTimerOn) System.currentTimeMillis() + (seconds * 1000) else 0L

            val message = Message(
                id = messageId,
                chatId = chatId,
                senderName = repository.getUsername() ?: "Self",
                encryptedPayload = encrypted,
                isIncoming = false,
                timestamp = System.currentTimeMillis(),
                isEphemeral = isTimerOn,
                ephemeralDurationSeconds = seconds,
                decimatedAt = decimatedTime
            )

            // Over the wire or loop back
            if (isVirtual) {
                // Instantly save ours
                repository.insertMessage(message)
                repository.updateChat(chat.copy(
                    lastMessageText = if (isTimerOn) "🔒 Ephemeral message" else trimmed,
                    lastMessageTime = System.currentTimeMillis()
                ))

                // Simulate bot processing & automated Response
                simulateVirtualPeerResponse(trimmed, chat, isTimerOn, seconds)
            } else {
                // Real TCP peer or global internet broker
                val success = AetherP2PEngine.sendChatMessage(
                    chatId = chatId,
                    messageId = messageId,
                    sender = repository.getUsername() ?: "unnamed",
                    encryptedPayload = encrypted,
                    isEphemeral = isTimerOn,
                    durationSeconds = seconds,
                    connectionType = chat.connectionType,
                    globalTopic = chat.connectionIp,
                    recipient = if (chat.peerUsername.startsWith("#")) null else chat.peerUsername
                )

                if (success) {
                    repository.insertMessage(message)
                    repository.updateChat(chat.copy(
                        lastMessageText = if (isTimerOn) "🔒 Ephemeral message" else trimmed,
                        lastMessageTime = System.currentTimeMillis()
                    ))
                } else {
                    addDiagnosticLog("Transmission rejected. Is peer disconnected?")
                }
            }
        }
    }

    // Automated Virtual Peer loopback answering E2EE
    private fun simulateVirtualPeerResponse(
        plainMsg: String,
        chat: Chat,
        isEphemeral: Boolean,
        durationSeconds: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(1000) // Aesthetic network latency simulation
            
            // Invoke Gemini API or Fallback Responder
            val systemInstr = """
                You are 'Sybil Q-Bot', a futuristic, high-protocol security terminal. 
                You chat inside a 100% decentralized end-to-end encrypted app called 'Aether'. 
                Keep replies extremely engaging, moderately brief, styled with quantum, cyberpunk, and cryptographic terms.
                Always speak with high-fidelity telemetry aesthetics.
            """.trimIndent()

            val replyPlain = GeminiClient.generateResponse(plainMsg, systemInstr)

            val sharedSecretKey = chat.sharedSecretBase64?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return@launch
            val responseEncrypted = AetherCrypto.encrypt(replyPlain, sharedSecretKey)

            val responseId = UUID.randomUUID().toString()
            val decimatedTime = if (isEphemeral) System.currentTimeMillis() + (durationSeconds * 1000) else 0L

            val botMsg = Message(
                id = responseId,
                chatId = chat.id,
                senderName = chat.peerUsername,
                encryptedPayload = responseEncrypted,
                isIncoming = true,
                timestamp = System.currentTimeMillis(),
                isEphemeral = isEphemeral,
                ephemeralDurationSeconds = durationSeconds,
                decimatedAt = decimatedTime
            )

            repository.insertMessage(botMsg)
            repository.updateChat(chat.copy(
                lastMessageText = if (isEphemeral) "🔒 Ephemeral transmission" else replyPlain,
                lastMessageTime = System.currentTimeMillis()
            ))
        }
    }

    // Establish link with virtual chatbot peer
    fun createVirtualPeerChat(botName: String = "Sybil Q-Bot") {
        viewModelScope.launch(Dispatchers.IO) {
            val keyPair = AetherCrypto.generateKeyPair()
            val ourPub = AetherCrypto.publicKeyToBase64(keyPair.public)
            val ourPriv = AetherCrypto.privateKeyToBase64(keyPair.private)

            // Generate temporary key pair for Bot representation
            val botKeyPair = AetherCrypto.generateKeyPair()
            val botPub = AetherCrypto.publicKeyToBase64(botKeyPair.public)

            // Calculate mutual derived secret key
            val secret = AetherCrypto.deriveSharedSecret(ourPriv, botPub)
            val secretBase64 = Base64.encodeToString(secret, Base64.NO_WRAP)

            val chat = Chat(
                id = "virtual_${botName.lowercase().replace(" ", "_")}",
                peerUsername = botName,
                peerPublicKeyBase64 = botPub,
                ourPrivateKeyBase64 = ourPriv,
                ourPublicKeyBase64 = ourPub,
                sharedSecretBase64 = secretBase64,
                isHost = true,
                connectionIp = "127.0.0.1",
                connectionPort = 0,
                connectionType = "VIRTUAL_PEER",
                isConnected = true,
                lastMessageText = "Quantum session established",
                lastMessageTime = System.currentTimeMillis()
            )

            repository.insertChat(chat)
            addDiagnosticLog("Simulated direct neural loopback created with $botName.")
        }
    }

    // Establish link with Remote P2P Host client over TCP socket
    fun initiateP2PConnection(ip: String, portStr: String) {
        val port = portStr.toIntOrNull() ?: 8999
        AetherP2PEngine.connectToPeer(ip, port, repository, viewModelScope)
    }

    // Establish link with Remote Peer over Global E2EE Internet Channel
    fun initiateGlobalConnection(channelName: String) {
        val sanitizedChannel = channelName.trim().lowercase().filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        if (sanitizedChannel.isEmpty()) return

        val topic = "aether_global_$sanitizedChannel"
        val chatId = "global_$sanitizedChannel"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Derive a stable, SHA-256 backed 256-bit AES key so channel communications are E2EE secured by channel name
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val keyBytes = digest.digest(sanitizedChannel.toByteArray(Charsets.UTF_8))
                val sharedSecretBase64 = Base64.encodeToString(keyBytes, Base64.NO_WRAP)

                val chat = Chat(
                    id = chatId,
                    peerUsername = "#$sanitizedChannel",
                    peerPublicKeyBase64 = "",
                    ourPrivateKeyBase64 = "",
                    ourPublicKeyBase64 = "",
                    sharedSecretBase64 = sharedSecretBase64,
                    isHost = false,
                    connectionIp = topic,
                    connectionPort = 0,
                    connectionType = "GLOBAL_INTERNET",
                    isConnected = true,
                    lastMessageText = "Global secure lobby ready.",
                    lastMessageTime = System.currentTimeMillis()
                )
                repository.insertChat(chat)
                addDiagnosticLog("Lobby connection for #$sanitizedChannel initialized.")
            } catch (e: Exception) {
                Log.e("AetherViewModel", "Failed to insert local channel record", e)
            }
        }

        AetherP2PEngine.connectToGlobalLobby(channelName, repository, viewModelScope)
    }

    // Tear down chat completely
    fun deleteChat(chat: Chat) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteChatById(chat.id)
            addDiagnosticLog("Chat index ${chat.peerUsername} deleted.")
        }
    }

    // Real-Time Ephemeral Purge Loop running on SQLite Room DB
    private fun startEphemeralPurgeDaemon() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val now = System.currentTimeMillis()
                val actives = repository.getActiveEphemeralMessages()
                for (msg in actives) {
                    if (now >= msg.decimatedAt) {
                        // Mark as disintegrated internally to trigger blur and crumble animations
                        repository.markMessageAsDecimated(msg.id)
                        
                        // Wipe permanently from disk after 2.5 seconds of nice visual collapse!
                        val messageId = msg.id
                        launch {
                            delay(2500)
                            repository.deleteMessage(messageId)
                        }
                    }
                }
                repository.clearDecimated() // periodic general cleanup of older residues
                delay(500) // check twice a second for precision disappearing triggers
            }
        }
    }

    fun getLocalIpAddress() = AetherP2PEngine.getLocalIpAddress()

    private fun addDiagnosticLog(message: String) {
        viewModelScope.launch {
            val list = _p2pDiagnosticLogs.value.toMutableList()
            // Keep last 15 system logs for clean retro terminal styling
            if (list.size > 15) list.removeAt(0)
            list.add("[${System.currentTimeMillis() % 100000}] $message")
            _p2pDiagnosticLogs.value = list
        }
    }

    override fun onCleared() {
        super.onCleared()
        AetherP2PEngine.stopAll()
    }
}

// Helper Sealed Class representation for direct internal screen navigation
sealed class Screen {
    object Onboarding : Screen()
    object ChatsList : Screen()
    data class ConversationByIP(val chatId: String) : Screen()
    object LinkCenter : Screen()
}
