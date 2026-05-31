package com.example.ui

import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crypto.AetherCrypto
import com.example.data.Chat
import com.example.data.Message
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Modifier.immersiveAmbientGlow(): Modifier = this.drawBehind {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.1f), Color.Transparent),
            center = Offset(size.width * 0.9f, size.height * 0.25f),
            radius = size.width * 0.7f
        ),
        radius = size.width * 0.7f,
        center = Offset(size.width * 0.9f, size.height * 0.25f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF6366F1).copy(alpha = 0.1f), Color.Transparent),
            center = Offset(size.width * 0.1f, size.height * 0.75f),
            radius = size.width * 0.7f
        ),
        radius = size.width * 0.7f,
        center = Offset(size.width * 0.1f, size.height * 0.75f)
    )
}

@Composable
fun AetherApp(viewModel: AetherViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianBackground
    ) {
        Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
            when (screen) {
                is Screen.Onboarding -> OnboardingScreen(viewModel)
                is Screen.ChatsList -> ChatsListScreen(viewModel)
                is Screen.ConversationByIP -> ConversationScreen(viewModel, screen.chatId)
                is Screen.LinkCenter -> LinkCenterScreen(viewModel)
            }
        }
    }
}

// 1. Onboarding Registration
@Composable
fun OnboardingScreen(viewModel: AetherViewModel) {
    var inputName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .immersiveAmbientGlow(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Futuristic Icon Space
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .background(NeonGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Cosmic Encryption Lock",
                    tint = NeonBlue,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "A E T H E R",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeonGreen,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.testTag("app_brand")
            )

            Text(
                text = "DECENTRALIZED CRYPTO TUNNEL",
                fontSize = 11.sp,
                color = NeonBlue,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "E2EE AES-GCM messenger with peer-constructed key negotiation & self-dissolving ephemeral timelines.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Enter quantum handle",
                fontSize = 12.sp,
                color = NeonBlue,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = inputName,
                onValueChange = { if (it.length <= 16) inputName = it },
                placeholder = { Text("username...", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("username_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = CyberCard,
                    focusedContainerColor = CyberGray,
                    unfocusedContainerColor = CyberGray
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.registerUser(inputName) },
                enabled = inputName.trim().isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("register_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    disabledContainerColor = CyberCard
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "GENERATE IDENTITY",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// 2. Chats Screen Dashboard
@Composable
fun ChatsListScreen(viewModel: AetherViewModel) {
    val username by viewModel.username.collectAsStateWithLifecycle()
    val fingerprint by viewModel.fingerprint.collectAsStateWithLifecycle()
    val allChats by viewModel.allChats.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val localIp = remember { viewModel.getLocalIpAddress() }

    var searchQuery by remember { mutableStateOf("") }

    val activeGlobalChannel = remember(allChats) {
        allChats.firstOrNull { it.connectionType == "GLOBAL_INTERNET" && it.isConnected }
            ?.let { chat ->
                if (chat.connectionIp.startsWith("aether_global_")) {
                    chat.connectionIp.removePrefix("aether_global_")
                } else {
                    chat.connectionIp
                }
            }
    }

    val filteredChats = remember(allChats, searchQuery) {
        if (searchQuery.isBlank()) {
            allChats
        } else {
            allChats.filter {
                it.peerUsername.contains(searchQuery, ignoreCase = true) ||
                it.connectionIp.contains(searchQuery, ignoreCase = true) ||
                it.connectionType.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(CyberGray.copy(alpha = 0.85f))
                    .drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // High-tech security badge
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonGreen.copy(alpha = 0.15f))
                                .border(1.dp, NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = NeonBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (activeGlobalChannel != null) "#$activeGlobalChannel" else "CipherNode",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = (-0.5).sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34D399)) // Emerald-400
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "P2P ACTIVE: SYSTEM ONLINE",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Profile card with user public identity fingerprint
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberCard)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonGreen.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = username ?: "unnamed",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Fingerprint: $fingerprint",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.navigateTo(Screen.LinkCenter) },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen.copy(alpha = 0.15f),
                            contentColor = NeonGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ CONNECT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        bottomBar = {
            // Futuristic bottom status banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberGray)
                    .drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .navigationBarsPadding()
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🔒 SECURE DISPERSED SYSTEM • END-TO-END AES-GCM",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBackground)
                .immersiveAmbientGlow()
        ) {
            if (allChats.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search connected peers or channels...", color = TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Peers",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                        focusedContainerColor = CyberCard,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                )
            }

            if (allChats.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = "No Connections Empty State",
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Vacuum Active",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Establish direct P2P connections or spawn an encrypted virtual chatbot to test telemetry.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.LinkCenter) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                    ) {
                        Text("OPEN QUANTUM HUB", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (filteredChats.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "No Results State",
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Peers Found",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No active channel or connection matches '$searchQuery'.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 8.dp)
                ) {
                    items(filteredChats, key = { it.id }) { chat ->
                        ChatListItem(chat = chat, onClick = {
                            viewModel.navigateTo(Screen.ConversationByIP(chat.id))
                        }, onDelete = {
                            viewModel.deleteChat(chat)
                        })
                    }
                }
            }
        }
    }
}

// Elegant row representing a Chat History element
@Composable
fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("chat_item_${chat.id}"),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Futuristic visual identity bubble with dynamic status color
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(CyberGray, Color(0xFF171B35))
                            )
                        )
                        .border(
                            1.dp,
                            if (chat.isConnected) NeonGreen else Color(0xFF2C2F45),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (chat.connectionType) {
                            "VIRTUAL_PEER" -> Icons.Default.Settings
                            "GLOBAL_INTERNET" -> Icons.Default.Lock
                            else -> Icons.Default.Person
                        },
                        contentDescription = null,
                        tint = if (chat.isConnected) NeonGreen else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = chat.peerUsername,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (chat.isConnected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = chat.lastMessageText ?: "No message packets",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(NeonBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            fontSize = 9.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Conversation",
                        tint = Color(0xFFFF5252).copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

    // 3. Link Center Screen
@Composable
fun LinkCenterScreen(viewModel: AetherViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Global Internet Lobby, 1: Local WiFi P2P
    var targetIp by remember { mutableStateOf("") }
    var targetPort by remember { mutableStateOf("8999") }
    var globalChannelName by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val localIp = remember { viewModel.getLocalIpAddress() }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberGray.copy(alpha = 0.85f))
                    .drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.ChatsList) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Aether Link Hub",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBackground)
                .immersiveAmbientGlow()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Tactical-Terminal Connection Mode Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("GLOBAL INTERNET LOBBY", "LOCAL P2P GRID").forEachIndexed { index, title ->
                        val selected = selectedTab == index
                        val accentColor = if (index == 0) NeonBlue else NeonGreen
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) accentColor.copy(alpha = 0.3f) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) accentColor else TextSecondary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Global Internet Channel Mode Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("GLOBAL ENCRYPTED LOBBY", fontSize = 11.sp, color = NeonBlue, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Choose a shared Channel ID (e.g. quantum-tunnel-5). Once you and your peer both enter it, the system will connect globally over the internet, run DH-E2EE key negotiation, and start a 100% private chat tunnel.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = globalChannelName,
                                onValueChange = { globalChannelName = it },
                                placeholder = { Text("Channel ID: e.g. private-chat-xyz", color = TextSecondary) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = NeonBlue,
                                    unfocusedBorderColor = CyberGray
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Popular public default channels row
                            Text(
                                text = "POPULAR PUBLIC CHANNELS", 
                                fontSize = 10.sp, 
                                color = NeonBlue, 
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("general-cyber", "quantum-lounge", "aether-secure").forEach { defaultChan ->
                                    val isSelected = globalChannelName.trim().lowercase() == defaultChan
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) NeonBlue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) NeonBlue else Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { globalChannelName = defaultChan }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#$defaultChan",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) NeonBlue else TextPrimary
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    if (globalChannelName.isNotBlank()) {
                                        viewModel.initiateGlobalConnection(globalChannelName.trim())
                                        viewModel.navigateTo(Screen.ChatsList)
                                    }
                                },
                                enabled = globalChannelName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonBlue,
                                    disabledContainerColor = Color.White.copy(alpha = 0.05f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("JOIN SECURE GLOBAL LOBBY", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Local listener card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("LOCAL HOST IP TELEMETRY", fontSize = 11.sp, color = NeonBlue, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "To let a peer connect, share this IP address & Port number. They must type it inside their 'Link Client' inputs.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("IP Address: $localIp", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                                Text("Listening Port: 8999", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Connect to Client Form
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("CONNECT TO REMOTE PEER", fontSize = 11.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = targetIp,
                                onValueChange = { targetIp = it },
                                placeholder = { Text("Peer IP: e.g. 192.168.1.10", color = TextSecondary) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = CyberGray
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = targetPort,
                                onValueChange = { targetPort = it },
                                placeholder = { Text("Port (Default: 8999)", color = TextSecondary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = CyberGray
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (targetIp.isNotBlank()) {
                                        viewModel.initiateP2PConnection(targetIp.trim(), targetPort.trim())
                                        viewModel.navigateTo(Screen.ChatsList)
                                    }
                                },
                                enabled = targetIp.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    disabledContainerColor = Color.White.copy(alpha = 0.05f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ESTABLISH PEER TUNNEL", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 4. Conversation Screen
@Composable
fun ConversationScreen(viewModel: AetherViewModel, chatId: String) {
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val allChats by viewModel.allChats.collectAsStateWithLifecycle()
    val activeChat = remember(allChats) { allChats.find { it.id == chatId } }
    val activeTimerSeconds by viewModel.activeEphemeralDuration.collectAsStateWithLifecycle()
    var inputMessageText by remember { mutableStateOf("") }
    val activeListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Automatically follow incoming logs scroll down
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            activeListState.animateScrollToItem(messages.size - 1)
        }
    }

    if (activeChat == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Tunnelling protocol collapsed.", color = Color.White)
        }
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .background(CyberGray.copy(alpha = 0.85f))
                    .drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.navigateTo(Screen.ChatsList) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonGreen.copy(alpha = 0.1f))
                                .border(
                                    1.dp,
                                    if (activeChat.isConnected) NeonGreen else Color(0xFF32354A),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (activeChat.connectionType == "VIRTUAL_PEER") Icons.Default.Settings else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (activeChat.isConnected) NeonGreen else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeChat.peerUsername,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (activeChat.isConnected) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF34D399)) // Emerald-400
                                    )
                                }
                            }
                            val keyHash = remember(activeChat.peerPublicKeyBase64) {
                                AetherCrypto.generateFingerprint(activeChat.peerPublicKeyBase64)
                            }
                            Text(
                                text = "Tunnel Fingerprint: $keyHash",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Encryption assurance blinker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "E2E Secure",
                            tint = NeonBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GCM-256",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonBlue,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Disappearing Ephemeral Timeline Configuration Row !
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Timer Settings",
                            tint = if (activeTimerSeconds > 0) NeonPurple else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EPHEMERAL Disappearing",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTimerSeconds > 0) NeonPurple else TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0 to "Off", 5 to "5s", 15 to "15s", 30 to "30s").forEach { (secs, label) ->
                            val isSelected = activeTimerSeconds == secs
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) NeonPurple else Color(0xFF141724))
                                    .clickable { viewModel.setEphemeralDuration(secs) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextPrimary
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBackground)
                .immersiveAmbientGlow()
        ) {
            // Lazy List showing complete message flow
            LazyColumn(
                state = activeListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // System Notification corresponding to Design HTML
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(100.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "END-TO-END ENCRYPTED VIA WEBTRC/GCM-P2P",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message, chat = activeChat)
                }
            }

            // Input panel (bottom row) styled like the design's "Futuristic Bottom Bar"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberGray)
                    .drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Attach payload",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    OutlinedTextField(
                        value = inputMessageText,
                        onValueChange = { inputMessageText = it },
                        placeholder = {
                            Text(
                                text = if (activeTimerSeconds > 0) "Ephemeral transmission..." else "Type an encrypted message...",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text")
                            .padding(horizontal = 4.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )

                    IconButton(
                        onClick = {
                            if (inputMessageText.trim().isNotEmpty()) {
                                viewModel.sendMessage(inputMessageText)
                                inputMessageText = ""
                            }
                        },
                        enabled = inputMessageText.trim().isNotEmpty(),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputMessageText.trim().isNotEmpty()) {
                                    if (activeTimerSeconds > 0) NeonPurple else NeonGreen
                                } else {
                                    Color.White.copy(alpha = 0.05f)
                                }
                            )
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (inputMessageText.trim().isNotEmpty()) Color.Black else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Under-input visual Home navigator pill mapping to HTML
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

// WhatsApp-like bubble but styled with cosmic cyberpunk overlays and real-time disintegration count down
@Composable
fun MessageBubble(message: Message, chat: Chat) {
    val isIncoming = message.isIncoming
    val alignment = if (isIncoming) Alignment.CenterStart else Alignment.CenterEnd

    // Dynamic decryption within the Composable to prove database contains only crypted byteframes !
    val decryptedText = remember(message.encryptedPayload) {
        val secretKey = chat.sharedSecretBase64?.let { Base64.decode(it, Base64.NO_WRAP) }
        if (secretKey != null) {
            AetherCrypto.decrypt(message.encryptedPayload, secretKey)
        } else {
            "[KEY_DERIVATION_FAILED]"
        }
    }

    // Active ticking timer state for visual feedback inside disappearing messages
    var remainingSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(message.isDecimated, message.decimatedAt) {
        if (message.isEphemeral && !message.isDecimated) {
            while (true) {
                val now = System.currentTimeMillis()
                val diff = message.decimatedAt - now
                if (diff <= 0) {
                    remainingSeconds = 0
                    break
                }
                remainingSeconds = (diff / 1000).toInt() + 1
                delay(200)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("message_bubble_${message.id}"),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isIncoming) Alignment.Start else Alignment.End
        ) {
            // Sender signature
            Text(
                text = "${message.senderName} • ${if (message.isIncoming) "INBOUND" else "OUTBOUND"}",
                fontSize = 9.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // Animated disintegrating box
            AnimatedContent(
                targetState = message.isDecimated,
                transitionSpec = {
                    fadeIn(animationSpec = tween(1000)) togetherWith fadeOut(animationSpec = tween(1500))
                },
                label = "disintegration"
            ) { isDecimated ->
                if (isDecimated) {
                    // Show glorious cosmic crumbling visual
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E0E0B))
                            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Wiped Out",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[COSMIC TELEMETRY TIMELINE PURGED]",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF5252),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    // Normal Encrypted message box
                    val backgroundBrush = if (isIncoming) {
                        Brush.verticalGradient(listOf(CyberCard, Color(0xFF1E2034)))
                    } else {
                        if (message.isEphemeral) {
                            Brush.verticalGradient(listOf(Color(0xFF38153A), Color(0xFF270E2F)))
                        } else {
                            Brush.verticalGradient(listOf(Color(0xFF0F3223), Color(0xFF0C241B)))
                        }
                    }

                    val glowBorderColor = if (message.isEphemeral) {
                        NeonPurple.copy(alpha = 0.4f)
                    } else {
                        if (isIncoming) NeonBlue.copy(alpha = 0.3f) else NeonGreen.copy(alpha = 0.4f)
                    }

                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isIncoming) 0.dp else 16.dp,
                                    bottomEnd = if (isIncoming) 16.dp else 0.dp
                                )
                            )
                            .background(backgroundBrush)
                            .border(
                                1.dp,
                                glowBorderColor,
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isIncoming) 0.dp else 16.dp,
                                    bottomEnd = if (isIncoming) 16.dp else 0.dp
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = decryptedText,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Padlock badge indicating E2E GCM check
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Shield Verified",
                                    tint = if (message.isEphemeral) NeonPurple else NeonBlue,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "E2EE AES-GCM",
                                    fontSize = 8.sp,
                                    color = if (message.isEphemeral) NeonPurple else NeonBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                if (message.isEphemeral) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Purging timer active",
                                        tint = NeonPurple,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${remainingSeconds}s",
                                        fontSize = 8.sp,
                                        color = NeonPurple,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
