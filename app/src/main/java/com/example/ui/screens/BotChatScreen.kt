package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BotChatMessage
import com.example.data.model.CryptoCurrency
import com.example.data.model.EmojiGameMode
import com.example.data.model.PlayerProfile
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TeleCyan
import com.example.ui.theme.VegasGold
import com.example.ui.viewmodel.AiAssistantState
import com.example.ui.viewmodel.CasinoViewModel
import com.example.ui.viewmodel.LiveBotConnectionState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BotChatScreen(
    profile: PlayerProfile,
    messages: List<BotChatMessage>,
    selectedEmojiGame: EmojiGameMode,
    emojiBetAmount: Double,
    isRollingEmoji: Boolean,
    lastEmojiAnimationValue: Pair<String, Int>?,
    liveBotState: LiveBotConnectionState,
    aiAssistantState: AiAssistantState = AiAssistantState.Idle,
    onSelectEmojiGame: (EmojiGameMode) -> Unit,
    onSetEmojiBet: (Double) -> Unit,
    onPlayEmojiGame: () -> Unit,
    onSendCommand: (String) -> Unit,
    onPollTelegram: () -> Unit,
    onUpdateChatId: (String) -> Unit,
    onTestBroadcast: (String) -> Unit,
    onClearChat: () -> Unit,
    onDismissAi: () -> Unit = {}
) {
    val activeCurrency = CryptoCurrency.fromCode(profile.activeCurrency)
    var commandText by remember { mutableStateOf("") }
    var showBotConfigDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showBotConfigDialog) {
        var chatIdDraft by remember(profile.botChatId) { mutableStateOf(profile.botChatId) }
        AlertDialog(
            onDismissRequest = { showBotConfigDialog = false },
            title = {
                Text(
                    text = "Live Telegram Bot API Bridge",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Status: ${liveBotState.lastSyncStatus}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (liveBotState.isTokenConfigured) NeonEmerald else VegasGold
                    )
                    Text(
                        text = "To broadcast real Telegram animated emojis (🎲 🎯 🎳 🏀 ⚽ 🎰) and game settlements to a live Telegram chat, set TELEGRAM_BOT_TOKEN in the AI Studio Secrets panel and enter your target Telegram chat_id below.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = chatIdDraft,
                        onValueChange = { chatIdDraft = it },
                        label = { Text("Telegram Chat ID (e.g. 123456789 or @channel)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            onTestBroadcast(chatIdDraft)
                            showBotConfigDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_telegram_broadcast_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🎲 Send Test Dice & Message via Bot API",
                            style = MaterialTheme.typography.labelLarge,
                            color = TeleCyan
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateChatId(chatIdDraft)
                        showBotConfigDialog = false
                    }
                ) {
                    Text("Save Chat ID")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBotConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // 1. Telegram Bot Header & In-Chat Emoji Game Launcher Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(TeleCyan, NeonEmerald))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🤖", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = liveBotState.connectedBotUsername ?: "@TeleVegasCasinoBot",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = liveBotState.lastSyncStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = TeleCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = onPollTelegram,
                            modifier = Modifier.testTag("poll_telegram_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.cd_poll_telegram),
                                tint = TeleCyan
                            )
                        }
                        IconButton(
                            onClick = { showBotConfigDialog = true },
                            modifier = Modifier.testTag("bot_config_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.cd_bot_settings),
                                tint = VegasGold
                            )
                        }
                        IconButton(
                            onClick = onClearChat,
                            modifier = Modifier.testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.cd_clear_chat),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Emoji Game Selector Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EmojiGameMode.entries.forEach { mode ->
                        val selected = mode == selectedEmojiGame
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectEmojiGame(mode) }
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) TeleCyan else ObsidianElevated,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .testTag("emoji_mode_${mode.name.lowercase()}"),
                            color = if (selected) TeleCyan.copy(alpha = 0.18f) else ObsidianSurfaceVariant,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = mode.emoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = mode.multiplierText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VegasGold
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Bet + Roll Button Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activeCurrency.defaultQuickBets.forEach { amt ->
                            val isSelected = emojiBetAmount == amt
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetEmojiBet(amt) },
                                label = {
                                    Text(
                                        text = "${CasinoViewModel.formatCrypto(amt, activeCurrency)} ${activeCurrency.code}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VegasGold.copy(alpha = 0.24f),
                                    selectedLabelColor = VegasGold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onPlayEmojiGame,
                        enabled = !isRollingEmoji,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("play_emoji_game_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonEmerald,
                            contentColor = Color(0xFF00210E)
                        )
                    ) {
                        Text(
                            text = if (isRollingEmoji) {
                                "Rolling ${selectedEmojiGame.emoji}..."
                            } else {
                                "Roll ${selectedEmojiGame.emoji} (${selectedEmojiGame.command})"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "${selectedEmojiGame.emoji} Rules: ${selectedEmojiGame.winThresholdDescription}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Animated Emoji Roll Banner when rolling
        AnimatedVisibility(
            visible = isRollingEmoji || lastEmojiAnimationValue != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val pair = lastEmojiAnimationValue
            if (pair != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianElevated
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = pair.first, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isRollingEmoji) {
                                    "Broadcasting animated ${pair.first} in chat..."
                                } else {
                                    "Last In-Chat ${pair.first} Roll Value: [${pair.second}]"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "PF Nonce #${profile.nonce}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TeleCyan
                        )
                    }
                }
            }
        }

        // Gemini AI Pit Boss Live Streaming Card
        AnimatedVisibility(
            visible = aiAssistantState !is AiAssistantState.Idle,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .border(1.dp, VegasGold.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF171329)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ Gemini 3.8 Flash • AI Pit Boss",
                            style = MaterialTheme.typography.labelLarge,
                            color = VegasGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelSmall,
                            color = TeleCyan,
                            modifier = Modifier
                                .clickable { onDismissAi() }
                                .testTag("dismiss_ai_chat_button")
                        )
                    }
                    val aiText = when (aiAssistantState) {
                        is AiAssistantState.Loading -> "Consulting Gemini 3.8 Flash Pit Boss..."
                        is AiAssistantState.Streaming -> aiAssistantState.text
                        is AiAssistantState.Success -> aiAssistantState.text
                        is AiAssistantState.Error -> "⚠️ ${aiAssistantState.message}"
                        AiAssistantState.Idle -> ""
                    }
                    Text(
                        text = aiText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (aiAssistantState is AiAssistantState.Error) CrimsonAlert else Color.White
                    )
                }
            }
        }

        // 2. Chat Message Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF070B12))
                .padding(horizontal = 10.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = null,
                                tint = TeleCyan,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No bot messages yet. Tap Roll ${selectedEmojiGame.emoji} or type /help!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(messages, key = { it.id.ifBlank { it.hashCode().toString() } }) { msg ->
                    TelegramMessageBubble(message = msg)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Inline Bot Command Keyboard Shortcuts
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickCommands = listOf(
                "/ai" to "✨ /ai Pit Boss",
                "/dice" to "🎲 /dice",
                "/darts" to "🎯 /darts",
                "/bowl" to "🎳 /bowl",
                "/hoops" to "🏀 /hoops",
                "/goal" to "⚽ /goal",
                "/slots" to "🎰 /slots",
                "/bj" to "🃏 /bj",
                "/balance" to "💎 /balance",
                "/help" to "ℹ️ /help"
            )
            quickCommands.forEach { (cmd, label) ->
                OutlinedButton(
                    onClick = { onSendCommand(cmd) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TeleCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Telegram Command Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandText,
                onValueChange = { commandText = it },
                placeholder = {
                    Text(
                        text = "Write a bot command (e.g. /dice 10, /slots 5, /help)...",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("bot_command_input"),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (commandText.isNotBlank()) {
                        onSendCommand(commandText)
                        commandText = ""
                    }
                },
                modifier = Modifier
                    .height(54.dp)
                    .testTag("bot_send_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeleCyan,
                    contentColor = Color(0xFF031725)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.cd_send_command)
                )
            }
        }
    }
}

@Composable
private fun TelegramMessageBubble(message: BotChatMessage) {
    val isUser = message.sender == "USER"
    val bubbleColor = when {
        isUser -> Color(0xFF15395B)
        message.outcome == "JACKPOT" -> Color(0xFF2E2205)
        message.outcome == "WIN" -> Color(0xFF0A2E1E)
        message.outcome == "LOSS" -> Color(0xFF28131A)
        else -> ObsidianSurface
    }
    val borderColor = when {
        isUser -> TeleCyan.copy(alpha = 0.5f)
        message.outcome == "JACKPOT" -> VegasGold
        message.outcome == "WIN" -> NeonEmerald
        message.outcome == "LOSS" -> CrimsonAlert.copy(alpha = 0.5f)
        else -> ObsidianElevated
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 330.dp)
                .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            color = bubbleColor
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = message.emojiSymbol, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isUser) "You" else "@TeleVegasCasinoBot",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isUser) TeleCyan else VegasGold
                        )
                    }
                    if (message.emojiValue > 0L) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x44000000)
                        ) {
                            Text(
                                text = "Roll #${message.emojiValue}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = message.commandOrText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )

                if (message.metadataSummary.isNotBlank()) {
                    Text(
                        text = message.metadataSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
