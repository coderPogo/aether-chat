package com.example.network

import android.content.Context
import android.util.Log
import com.example.crypto.AetherCrypto
import com.example.data.Chat
import com.example.data.Message
import com.example.data.AetherRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.*

object AetherP2PEngine {
    private const val TAG = "AetherP2PEngine"
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private val activeConnections = Collections.synchronizedMap(mutableMapOf<String, SocketInfo>())

    // Unique device session ID to filter out echoed messages from ntfy pub-sub broker
    val deviceUniqueId: String = UUID.randomUUID().toString()

    // Store active global channel WebSockets
    private val activeGlobalLobbies = Collections.synchronizedMap(mutableMapOf<String, WebSocket>())

    // Channel to notify the view model / app of incoming updates
    private val _events = MutableSharedFlow<P2PEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<P2PEvent> = _events

    data class SocketInfo(
        val socket: Socket,
        val writer: PrintWriter,
        val runJob: Job,
        var peerUsername: String? = null,
        var peerPubKey: String? = null
    )

    sealed class P2PEvent {
        data class ConnectionEstablished(val chatId: String, val peerUsername: String, val peerPubKey: String) : P2PEvent()
        data class ConnectionClosed(val chatId: String) : P2PEvent()
        data class MessageReceived(val messageId: String, val chatId: String, val senderName: String, val encryptedPayload: String, val isEphemeral: Boolean, val ephemeralDurationSeconds: Int) : P2PEvent()
        data class Error(val text: String) : P2PEvent()
        data class Diagnostic(val log: String) : P2PEvent()
    }

    // Retrieve the device's local IPv4 Address on the current Wi-Fi interface
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val ip = address.hostAddress
                        if (ip != null && !ip.startsWith("127.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving IP Address", e)
        }
        return "127.0.0.1"
    }

    // Start Raw TCP Listening Socket (WebRTC signaling alternative / Direct connection server)
    fun startServer(context: Context, port: Int = 8999, repository: AetherRepository, scope: CoroutineScope) {
        if (serverJob != null && serverJob?.isActive == true) {
            emitDiagnostic("Server already listening on port $port")
            return
        }

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                emitDiagnostic("Quantum Listener ONLINE: ${getLocalIpAddress()}:$port")

                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    emitDiagnostic("Incoming hand-shake connection from: ${socket.inetAddress.hostAddress}")

                    // Spawning handler for incoming peer socket connection
                    val connectionId = UUID.randomUUID().toString()
                    val job = launch {
                        handleClientSocket(socket, connectionId, repository)
                    }
                }
            } catch (e: Exception) {
                emitDiagnostic("Listener socket error: ${e.message}")
            }
        }
    }

    // Stop listening server and all peer sockets
    fun stopAll() {
        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
        } catch (e: java.io.IOException) {
            // ignore
        }
        serverSocket = null

        synchronized(activeConnections) {
            activeConnections.forEach { (_, info) ->
                try {
                    info.writer.close()
                    info.socket.close()
                } catch (e: Exception) {
                    // ignore
                }
                info.runJob.cancel()
            }
            activeConnections.clear()
        }
        emitDiagnostic("Aether Net completely shutdown.")
    }

    // Connect to a remote host (Client Mode)
    fun connectToPeer(
        peerIp: String,
        port: Int,
        repository: AetherRepository,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            emitDiagnostic("Initiating P2P quantum tunnel to $peerIp:$port...")
            try {
                val socket = Socket(peerIp, port)
                val connectionId = UUID.randomUUID().toString()

                val job = launch {
                    handleClientSocket(socket, connectionId, repository)
                }
            } catch (e: Exception) {
                emitDiagnostic("Tunnel initiation failed: ${e.message}")
                _events.emit(P2PEvent.Error("Connection Failed: Unable to connect to host."))
            }
        }
    }

    // Core read-write frame engine
    private suspend fun handleClientSocket(socket: Socket, connectionId: String, repository: AetherRepository) {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        val writer = PrintWriter(socket.getOutputStream(), true)

        var currentChatId: String? = null

        val socketInfo = SocketInfo(
            socket = socket,
            writer = writer,
            runJob = CoroutineScope(Dispatchers.IO).launch { } // placeholder
        )
        activeConnections[connectionId] = socketInfo

        try {
            // Phase 1: Share Key Exchange as soon as we connect or accept
            val ourUsername = repository.getUsername() ?: "unnamed"
            val ourKeyPair = AetherCrypto.generateKeyPair() // Ephemeral KeyPair for this conversation
            val ourPubBase64 = AetherCrypto.publicKeyToBase64(ourKeyPair.public)
            val ourPrivBase64 = AetherCrypto.privateKeyToBase64(ourKeyPair.private)

            // Send handshake frame
            val shakeFrame = "type:key_exchange|username:$ourUsername|public_key:$ourPubBase64"
            writer.println(shakeFrame)
            emitDiagnostic("Handshake transmitted. Awaiting key payload...")

            // Read loop
            var line: String?
            while (true) {
                line = withContext(Dispatchers.IO) { reader.readLine() } ?: break
                if (line.isBlank()) continue

                val parts = line.split("|").associate {
                    val keyVal = it.split(":", limit = 2)
                    if (keyVal.size == 2) keyVal[0] to keyVal[1] else "" to ""
                }

                when (parts["type"]) {
                    "key_exchange" -> {
                        val peerUsername = parts["username"] ?: "anonymous"
                        val peerPubKey = parts["public_key"] ?: ""

                        if (peerPubKey.isNotBlank()) {
                            // Compute peer ID
                            val chatId = peerUsername // Use peer username as unique chatId
                            currentChatId = chatId

                            // Save socket info to active connections mapped to this chat
                            socketInfo.peerUsername = peerUsername
                            socketInfo.peerPubKey = peerPubKey

                            // Calculate shared secret using ephemeral ECDH
                            val sharedSecret = AetherCrypto.deriveSharedSecret(ourPrivBase64, peerPubKey)
                            val sharedSecretBase64 = android.util.Base64.encodeToString(sharedSecret, android.util.Base64.NO_WRAP)

                            // Save Chat in SQLite database (Room)
                            val chat = Chat(
                                id = chatId,
                                peerUsername = peerUsername,
                                peerPublicKeyBase64 = peerPubKey,
                                ourPrivateKeyBase64 = ourPrivBase64,
                                ourPublicKeyBase64 = ourPubBase64,
                                sharedSecretBase64 = sharedSecretBase64,
                                isHost = socketInfo.socket.localPort == 8999, // if we are the server
                                connectionIp = socket.inetAddress.hostAddress ?: "",
                                connectionPort = socket.port,
                                connectionType = if (socketInfo.socket.localPort == 8999) "P2P_SERVER" else "P2P_CLIENT",
                                isConnected = true,
                                lastMessageText = "Quantum session established",
                                lastMessageTime = System.currentTimeMillis()
                            )
                            repository.insertChat(chat)

                            // Store in global active connections so we can find it by chatId
                            activeConnections[chatId] = socketInfo

                            emitDiagnostic("Tunnel SECURE with $peerUsername! [E2EE: AES-GCM-256 Enabled]")
                            _events.emit(P2PEvent.ConnectionEstablished(chatId, peerUsername, peerPubKey))
                        }
                    }
                    "chat_message" -> {
                        val msgId = parts["id"] ?: UUID.randomUUID().toString()
                        val senderName = parts["sender"] ?: "anonymous"
                        val payload = parts["payload"] ?: ""
                        val isEphemeral = parts["is_ephemeral"] == "true"
                        val epSeconds = parts["duration_seconds"]?.toIntOrNull() ?: 10
                        val chatId = currentChatId ?: senderName

                        if (payload.isNotBlank()) {
                            _events.emit(P2PEvent.MessageReceived(
                                messageId = msgId,
                                chatId = chatId,
                                senderName = senderName,
                                encryptedPayload = payload,
                                isEphemeral = isEphemeral,
                                ephemeralDurationSeconds = epSeconds
                            ))
                        }
                    }
                    "keep_alive" -> {
                        // ignore heartbeat
                    }
                }
            }
        } catch (e: Exception) {
            emitDiagnostic("P2P Socket closed/errored: ${e.message}")
        } finally {
            try {
                reader.close()
                writer.close()
                socket.close()
            } catch (e: Exception) {
                // Ignore close error
            }
            activeConnections.remove(connectionId)
            currentChatId?.let { chatId ->
                activeConnections.remove(chatId)
                repository.getChatById(chatId)?.let { c ->
                    repository.updateChat(c.copy(isConnected = false))
                }
                _events.emit(P2PEvent.ConnectionClosed(chatId))
            }
            emitDiagnostic("Quantum peer disconnected.")
        }
    }

    // Connect to a global E2EE channel lobby over the internet (via ntfy sub stream)
    fun connectToGlobalLobby(
        channelName: String,
        repository: AetherRepository,
        scope: CoroutineScope
    ) {
        val sanitizedChannel = channelName.trim().lowercase().filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        if (sanitizedChannel.isEmpty()) return

        val topic = "aether_global_$sanitizedChannel"
        emitDiagnostic("Connecting to Global Internet Grid on channel: $sanitizedChannel")

        scope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .build()

                val request = Request.Builder()
                    .url("wss://ntfy.sh/$topic/ws")
                    .build()

                val ourUsername = repository.getUsername() ?: "unnamed"
                val ourKeyPair = AetherCrypto.generateKeyPair()
                val ourPubBase64 = AetherCrypto.publicKeyToBase64(ourKeyPair.public)
                val ourPrivBase64 = AetherCrypto.privateKeyToBase64(ourKeyPair.private)

                var webSocketRef: WebSocket? = null

                val listener = object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        webSocketRef = webSocket
                        emitDiagnostic("Global lobby channel online. Broadcasting identity signature...")

                        // Store this lobby in active lobbies
                        activeGlobalLobbies[topic] = webSocket

                        // Publish our handshake frame immediately
                        publishToGlobalLobby(topic, "type:key_exchange|username:$ourUsername|public_key:$ourPubBase64|device_id:$deviceUniqueId")
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        try {
                            val json = JSONObject(text)
                            val event = json.optString("event")
                            if (event == "message") {
                                val messageContent = json.optString("message")
                                if (messageContent.isBlank()) return

                                val parts = messageContent.split("|").associate {
                                    val keyVal = it.split(":", limit = 2)
                                    if (keyVal.size == 2) keyVal[0] to keyVal[1] else "" to ""
                                }

                                val senderId = parts["device_id"] ?: ""
                                if (senderId == deviceUniqueId) {
                                    // Ignore our own published frames echoed back to us
                                    return
                                }

                                scope.launch(Dispatchers.IO) {
                                    when (parts["type"]) {
                                        "key_exchange" -> {
                                            val peerUsername = parts["username"] ?: "anonymous"
                                            val peerPubKey = parts["public_key"] ?: ""

                                            if (peerPubKey.isNotBlank()) {
                                                val chatId = "global_${sanitizedChannel}_$peerUsername"

                                                // Calculate shared secret using ephemeral ECDH
                                                val sharedSecret = AetherCrypto.deriveSharedSecret(ourPrivBase64, peerPubKey)
                                                val sharedSecretBase64 = android.util.Base64.encodeToString(sharedSecret, android.util.Base64.NO_WRAP)

                                                // Save Chat in SQLite database (Room)
                                                val chat = Chat(
                                                    id = chatId,
                                                    peerUsername = peerUsername,
                                                    peerPublicKeyBase64 = peerPubKey,
                                                    ourPrivateKeyBase64 = ourPrivBase64,
                                                    ourPublicKeyBase64 = ourPubBase64,
                                                    sharedSecretBase64 = sharedSecretBase64,
                                                    isHost = false,
                                                    connectionIp = topic, // store topic as connection ip
                                                    connectionPort = 0,
                                                    connectionType = "GLOBAL_INTERNET",
                                                    isConnected = true,
                                                    lastMessageText = "Global E2EE session established",
                                                    lastMessageTime = System.currentTimeMillis()
                                                )
                                                repository.insertChat(chat)

                                                // Map both the actual chatId and the topic to this connection
                                                activeGlobalLobbies[chatId] = webSocket
                                                
                                                emitDiagnostic("E2EE secure tunnel ready with $peerUsername globally!")
                                                _events.emit(P2PEvent.ConnectionEstablished(chatId, peerUsername, peerPubKey))

                                                // Reply to complete handshake if not already a reply
                                                if (parts["is_reply"] != "true") {
                                                    publishToGlobalLobby(
                                                        topic,
                                                        "type:key_exchange|username:$ourUsername|public_key:$ourPubBase64|device_id:$deviceUniqueId|is_reply:true"
                                                    )
                                                }
                                            }
                                        }
                                        "chat_message" -> {
                                            val msgId = parts["id"] ?: UUID.randomUUID().toString()
                                            val senderName = parts["sender"] ?: "anonymous"
                                            val payload = parts["payload"] ?: ""
                                            val isEphemeral = parts["is_ephemeral"] == "true"
                                            val epSeconds = parts["duration_seconds"]?.toIntOrNull() ?: 10
                                            val chatId = "global_${sanitizedChannel}_$senderName"

                                            if (payload.isNotBlank()) {
                                                _events.emit(P2PEvent.MessageReceived(
                                                    messageId = msgId,
                                                    chatId = chatId,
                                                    senderName = senderName,
                                                    encryptedPayload = payload,
                                                    isEphemeral = isEphemeral,
                                                    ephemeralDurationSeconds = epSeconds
                                                ))
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in webSocket onMessage matching JSON", e)
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        emitDiagnostic("Global connection interrupted: ${t.message}")
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        emitDiagnostic("Global connection closed offline.")
                    }
                }

                client.newWebSocket(request, listener)
            } catch (e: Exception) {
                emitDiagnostic("Global lobby initiation failed: ${e.message}")
                _events.emit(P2PEvent.Error("Connection Failed: Unable to connect to global net."))
            }
        }
    }

    // Publish helper to POST plaintext protocol payloads to ntfy.sh REST endpoint
    fun publishToGlobalLobby(topic: String, payload: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://ntfy.sh/$topic")
            .post(payload.toRequestBody())
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                Log.e(TAG, "ntfy.sh publish failure", e)
            }
            override fun onResponse(call: okhttp3.Call, response: Response) {
                response.close()
            }
        })
    }

    // Send an encrypted chat message over direct TCP or global internet channel
    fun sendChatMessage(
        chatId: String,
        messageId: String,
        sender: String,
        encryptedPayload: String,
        isEphemeral: Boolean,
        durationSeconds: Int,
        connectionType: String = "P2P",
        globalTopic: String? = null
    ): Boolean {
        if (connectionType == "GLOBAL_INTERNET") {
            val topic = globalTopic ?: return false
            val frame = "type:chat_message|id:$messageId|sender:$sender|payload:$encryptedPayload|is_ephemeral:$isEphemeral|duration_seconds:$durationSeconds|device_id:$deviceUniqueId"
            publishToGlobalLobby(topic, frame)
            return true
        }

        val connection = activeConnections[chatId] ?: return false
        return try {
            val frame = "type:chat_message|id:$messageId|sender:$sender|payload:$encryptedPayload|is_ephemeral:$isEphemeral|duration_seconds:$durationSeconds"
            CoroutineScope(Dispatchers.IO).launch {
                connection.writer.println(frame)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun emitDiagnostic(log: String) {
        Log.d(TAG, log)
        CoroutineScope(Dispatchers.IO).launch {
            _events.emit(P2PEvent.Diagnostic(log))
        }
    }
}
