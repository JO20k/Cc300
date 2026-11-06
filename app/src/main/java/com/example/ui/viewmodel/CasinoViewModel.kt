package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BlackjackStage
import com.example.data.model.BlackjackState
import com.example.data.model.BotChatMessage
import com.example.data.model.CryptoCurrency
import com.example.data.model.CryptoTransaction
import com.example.data.model.EmojiGameMode
import com.example.data.model.PlayerProfile
import com.example.data.model.PlayingCard
import com.example.data.model.RouletteBetType
import com.example.data.model.RouletteState
import com.example.data.model.SlotSymbol
import com.example.data.model.SlotsState
import com.example.data.remote.TelegramBotApiService
import com.example.data.remote.TelegramBotClient
import com.example.data.repository.CasinoRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.Locale
import kotlin.math.abs

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiAssistantState {
    data object Idle : AiAssistantState
    data object Loading : AiAssistantState
    data class Streaming(val text: String) : AiAssistantState
    data class Success(val text: String) : AiAssistantState
    data class Error(val message: String) : AiAssistantState
}

data class LiveBotConnectionState(
    val isTokenConfigured: Boolean = false,
    val connectedBotUsername: String? = null,
    val connectedBotName: String? = null,
    val lastSyncStatus: String = "Local Bot Engine Active",
    val isSyncing: Boolean = false
)

class CasinoViewModel(
    private val repository: CasinoRepository,
    private val currentUserId: String,
    private val defaultDisplayName: String = "TelePlayer",
    private val telegramClient: TelegramBotClient = TelegramBotClient()
) : ViewModel() {

    val profileState: StateFlow<UiState<PlayerProfile>> = repository.observeProfile(currentUserId)
        .map<PlayerProfile?, UiState<PlayerProfile>> { profile ->
            if (profile != null) {
                UiState.Success(profile)
            } else {
                UiState.Loading
            }
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load player wallet"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val messagesState: StateFlow<UiState<List<BotChatMessage>>> = repository.observeMessages(currentUserId)
        .map<List<BotChatMessage>, UiState<List<BotChatMessage>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing messages", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load bot chat messages"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val transactionsState: StateFlow<UiState<List<CryptoTransaction>>> = repository.observeTransactions(currentUserId)
        .map<List<CryptoTransaction>, UiState<List<CryptoTransaction>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing transactions", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load crypto ledger"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _selectedEmojiGame = MutableStateFlow(EmojiGameMode.DICE)
    val selectedEmojiGame: StateFlow<EmojiGameMode> = _selectedEmojiGame.asStateFlow()

    private val _emojiBetAmount = MutableStateFlow(5.0)
    val emojiBetAmount: StateFlow<Double> = _emojiBetAmount.asStateFlow()

    private val _isRollingEmoji = MutableStateFlow(false)
    val isRollingEmoji: StateFlow<Boolean> = _isRollingEmoji.asStateFlow()

    private val _lastEmojiAnimationValue = MutableStateFlow<Pair<String, Int>?>(null)
    val lastEmojiAnimationValue: StateFlow<Pair<String, Int>?> = _lastEmojiAnimationValue.asStateFlow()

    private val _blackjackState = MutableStateFlow(BlackjackState())
    val blackjackState: StateFlow<BlackjackState> = _blackjackState.asStateFlow()

    private val _slotsState = MutableStateFlow(SlotsState())
    val slotsState: StateFlow<SlotsState> = _slotsState.asStateFlow()

    private val _rouletteState = MutableStateFlow(RouletteState())
    val rouletteState: StateFlow<RouletteState> = _rouletteState.asStateFlow()

    private val _liveBotState = MutableStateFlow(LiveBotConnectionState())
    val liveBotState: StateFlow<LiveBotConnectionState> = _liveBotState.asStateFlow()

    private val _bannerNotification = MutableStateFlow<String?>(null)
    val bannerNotification: StateFlow<String?> = _bannerNotification.asStateFlow()

    private val _aiAssistantState = MutableStateFlow<AiAssistantState>(AiAssistantState.Idle)
    val aiAssistantState: StateFlow<AiAssistantState> = _aiAssistantState.asStateFlow()

    private val geminiModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private var lastUpdateOffset: Long? = null

    init {
        initializeUserAndBot()
    }

    private fun initializeUserAndBot() {
        viewModelScope.launch {
            val existing = repository.getProfile(currentUserId).getOrNull()
            if (existing == null) {
                val created = repository.createInitialProfile(
                    username = defaultDisplayName,
                    tonBalance = 250.0,
                    usdtBalance = 1000.0,
                    btcBalance = 0.05
                ).getOrNull()
                if (created != null) {
                    repository.recordTransaction(
                        txType = "FAUCET",
                        currency = "TON",
                        amount = 250.0,
                        txHash = "0x${CasinoRepository.sha256Hex("welcome_$currentUserId").take(32)}",
                        addressOrNote = "Welcome VIP Starter Pack (250 TON + 1,000 USDT + 0.05 BTC)"
                    )
                    repository.recordMessage(
                        sender = "BOT",
                        commandOrText = "👋 Welcome to @TeleVegasCasinoBot, ${created.username}!\n" +
                            "💎 Starter Vault Credited: 250.00 TON | 1,000.00 USDT | 0.0500 BTC\n" +
                            "🎯 Tap any In-Chat Emoji Game below (🎲 🎯 🎳 🏀 ⚽ 🎰) or type /help, /dice 10, /slots 5, /bj 10, /roulette red 10!",
                        gameType = "NONE",
                        emojiSymbol = "🤖",
                        emojiValue = 0L,
                        currency = "TON",
                        outcome = "INFO",
                        metadataSummary = "Provably Fair SHA-256 Active"
                    )
                }
            }
            checkLiveTelegramBotStatus()
        }
    }

    fun checkLiveTelegramBotStatus() {
        if (!telegramClient.isTokenConfigured) {
            _liveBotState.value = LiveBotConnectionState(
                isTokenConfigured = false,
                lastSyncStatus = "Simulator + Cloud Ledger Mode (Set TELEGRAM_BOT_TOKEN in Secrets for live Bot API)"
            )
            return
        }
        viewModelScope.launch {
            _liveBotState.update { it.copy(isTokenConfigured = true, isSyncing = true) }
            telegramClient.verifyBotIdentity()
                .onSuccess { bot ->
                    telegramClient.registerCasinoCommands()
                    _liveBotState.value = LiveBotConnectionState(
                        isTokenConfigured = true,
                        connectedBotUsername = "@${bot.username ?: "TeleVegasBot"}",
                        connectedBotName = bot.firstName,
                        lastSyncStatus = "Connected to Live Telegram Bot @${bot.username ?: bot.firstName}",
                        isSyncing = false
                    )
                }
                .onFailure { error ->
                    _liveBotState.value = LiveBotConnectionState(
                        isTokenConfigured = true,
                        lastSyncStatus = "Telegram API: ${error.localizedMessage ?: "Check TELEGRAM_BOT_TOKEN"}",
                        isSyncing = false
                    )
                }
        }
    }

    fun sendTestTelegramBroadcast(targetChatId: String) {
        val cleanChatId = targetChatId.trim()
        if (cleanChatId.isBlank()) {
            showBanner("Enter a valid Telegram chat_id first to send a live test broadcast.")
            return
        }
        updateBotChatId(cleanChatId)
        if (!telegramClient.isTokenConfigured) {
            showBanner("Saved Chat ID ($cleanChatId). Configure TELEGRAM_BOT_TOKEN in Secrets to enable live HTTP API calls.")
            return
        }

        viewModelScope.launch {
            _liveBotState.update { it.copy(isSyncing = true) }
            val diceResult = telegramClient.sendEmojiDiceRoll(chatId = cleanChatId, emoji = "🎲")
            val msgResult = telegramClient.sendCasinoMessage(
                chatId = cleanChatId,
                htmlText = "<b>🤖 @TeleVegasCasinoBot Connected!</b>\n" +
                    "Live Telegram Bot API bridge is active for chat <code>$cleanChatId</code>.\n" +
                    "Dice test outcome: <b>${diceResult.getOrNull()?.value ?: "N/A"}</b>",
                includeInlineKeyboard = true
            )
            if (msgResult.isSuccess) {
                val rolled = diceResult.getOrNull()?.value ?: 6
                _liveBotState.update {
                    it.copy(
                        isSyncing = false,
                        lastSyncStatus = "Sent live 🎲 (Rolled $rolled) & message to $cleanChatId"
                    )
                }
                showBanner("Live Telegram broadcast succeeded! Dice rolled $rolled in chat $cleanChatId.")
            } else {
                val err = msgResult.exceptionOrNull()?.localizedMessage ?: "Telegram API error"
                _liveBotState.update {
                    it.copy(isSyncing = false, lastSyncStatus = "Broadcast failed: $err")
                }
                showBanner("Telegram API Error: $err")
            }
        }
    }

    fun pollLiveTelegramUpdates() {
        if (!telegramClient.isTokenConfigured) {
            showBanner("Add TELEGRAM_BOT_TOKEN in AI Studio Secrets to poll live Telegram chats.")
            return
        }
        viewModelScope.launch {
            _liveBotState.update { it.copy(isSyncing = true) }
            telegramClient.fetchUpdates(offset = lastUpdateOffset)
                .onSuccess { updates ->
                    if (updates.isEmpty()) {
                        _liveBotState.update {
                            it.copy(isSyncing = false, lastSyncStatus = "Polled Telegram: No new incoming messages")
                        }
                    } else {
                        for (update in updates) {
                            lastUpdateOffset = update.updateId + 1
                            val msg = update.message ?: continue
                            val chatId = msg.chat?.id?.toString().orEmpty()
                            if (chatId.isNotBlank()) {
                                updateBotChatId(chatId)
                            }
                            val text = msg.text ?: msg.dice?.let { "${it.emoji} (Rolled ${it.value})" } ?: continue
                            handleCommandInput(text, fromRemoteTelegram = true)
                        }
                        _liveBotState.update {
                            it.copy(isSyncing = false, lastSyncStatus = "Synced ${updates.size} live Telegram update(s)")
                        }
                    }
                }
                .onFailure { e ->
                    _liveBotState.update {
                        it.copy(isSyncing = false, lastSyncStatus = "Telegram sync error: ${e.localizedMessage}")
                    }
                }
        }
    }

    fun dismissBanner() {
        _bannerNotification.value = null
    }

    private fun showBanner(message: String) {
        _bannerNotification.value = message
    }

    private fun currentProfileOrNull(): PlayerProfile? {
        return (profileState.value as? UiState.Success)?.data
    }

    fun selectCurrency(currency: CryptoCurrency) {
        val profile = currentProfileOrNull() ?: return
        val quickDefault = currency.defaultQuickBets.getOrElse(1) { 1.0 }
        _emojiBetAmount.value = quickDefault
        _blackjackState.update { it.copy(betAmount = quickDefault) }
        _slotsState.update { it.copy(betAmount = quickDefault) }
        _rouletteState.update { it.copy(betAmount = quickDefault) }
        viewModelScope.launch {
            repository.updateProfile(profile.copy(activeCurrency = currency.code))
        }
    }

    fun selectEmojiGame(mode: EmojiGameMode) {
        _selectedEmojiGame.value = mode
    }

    fun setEmojiBetAmount(amount: Double) {
        _emojiBetAmount.value = amount.coerceAtLeast(0.0001)
    }

    fun updateBotChatId(chatId: String) {
        val profile = currentProfileOrNull() ?: return
        viewModelScope.launch {
            repository.updateProfile(profile.copy(botChatId = chatId.trim()))
            showBanner("Saved Telegram Chat ID: ${chatId.trim().ifBlank { "Cleared" }}")
        }
    }

    fun updateClientSeed(newClientSeed: String) {
        val profile = currentProfileOrNull() ?: return
        val clean = newClientSeed.trim().take(64).ifBlank { "televegas_seed" }
        val newServerSeedHash = CasinoRepository.sha256Hex("server_${clean}_${System.currentTimeMillis()}")
        viewModelScope.launch {
            repository.updateProfile(
                profile.copy(
                    clientSeed = clean,
                    serverSeedHash = newServerSeedHash,
                    nonce = 1L
                )
            )
            showBanner("Rotated Provably Fair Client Seed & Server Seed Hash!")
        }
    }

    private fun generateProvablyFairInt(profile: PlayerProfile, maxInclusive: Int, salt: String = ""): Pair<Int, String> {
        val input = "${profile.serverSeedHash}:${profile.clientSeed}:${profile.nonce}:$salt:${System.nanoTime()}"
        val hash = CasinoRepository.sha256Hex(input)
        val hexSlice = hash.take(8)
        val rawValue = hexSlice.toLong(16)
        val roll = (abs(rawValue) % maxInclusive).toInt() + 1
        return roll to hash.take(16)
    }

    // =========================================================================
    // 1. IN-CHAT EMOJI GAMES & BOT COMMAND HANDLER
    // =========================================================================

    fun playEmojiGame(
        mode: EmojiGameMode = _selectedEmojiGame.value,
        customBet: Double? = null
    ) {
        if (_isRollingEmoji.value) return
        val profile = currentProfileOrNull() ?: return
        val currency = CryptoCurrency.fromCode(profile.activeCurrency)
        val bet = (customBet ?: _emojiBetAmount.value).coerceAtLeast(0.0001)
        val currentBal = profile.balanceFor(currency)
        if (bet > currentBal) {
            showBanner("Insufficient ${currency.code} balance! Claim free ${currency.code} in the Wallet tab.")
            return
        }

        viewModelScope.launch {
            _isRollingEmoji.value = true
            val userCommand = "${mode.command} ${formatCrypto(bet, currency)}"
            repository.recordMessage(
                sender = "USER",
                commandOrText = "$userCommand ${currency.code}",
                gameType = mode.gameTypeKey,
                emojiSymbol = mode.emoji,
                emojiValue = 0L,
                betAmount = bet,
                payoutAmount = 0.0,
                currency = currency.code,
                outcome = "ACTIVE",
                metadataSummary = "Sending animated ${mode.emoji} via Telegram Bot Engine..."
            )

            // If live Telegram Bot API is configured with a chat_id, call real sendDice API
            val liveDice = if (telegramClient.isTokenConfigured && profile.botChatId.isNotBlank()) {
                telegramClient.sendEmojiDiceRoll(chatId = profile.botChatId, emoji = mode.emoji).getOrNull()
            } else {
                null
            }

            val (provableRoll, roundHash) = generateProvablyFairInt(profile, mode.maxRoll, mode.gameTypeKey)
            val finalRoll = liveDice?.value ?: provableRoll
            val sourceTag = if (liveDice != null) "Telegram API sendDice" else "PF #$roundHash"

            _lastEmojiAnimationValue.value = mode.emoji to finalRoll
            delay(550)

            val multiplier = when (mode) {
                EmojiGameMode.DICE -> when (finalRoll) {
                    6 -> 3.0
                    4, 5 -> 1.8
                    else -> 0.0
                }
                EmojiGameMode.DARTS -> when (finalRoll) {
                    6 -> 4.0
                    5 -> 2.0
                    4 -> 1.2
                    else -> 0.0
                }
                EmojiGameMode.BOWLING -> when (finalRoll) {
                    6 -> 3.5
                    5 -> 1.9
                    else -> 0.0
                }
                EmojiGameMode.BASKETBALL -> when (finalRoll) {
                    5 -> 3.2
                    4 -> 2.0
                    else -> 0.0
                }
                EmojiGameMode.FOOTBALL -> when (finalRoll) {
                    5 -> 3.0
                    4 -> 1.9
                    3 -> 1.0
                    else -> 0.0
                }
            }

            val payout = bet * multiplier
            val outcome = when {
                multiplier >= 3.0 -> "JACKPOT"
                multiplier > 1.0 -> "WIN"
                multiplier == 1.0 -> "PUSH"
                else -> "LOSS"
            }

            val resultDescription = when (mode) {
                EmojiGameMode.DICE -> "🎲 Dice landed on [$finalRoll / 6]"
                EmojiGameMode.DARTS -> if (finalRoll == 6) "🎯 BULLSEYE! [6 / 6]" else "🎯 Dart hit ring [$finalRoll / 6]"
                EmojiGameMode.BOWLING -> if (finalRoll == 6) "🎳 STRIKE! All pins down [6 / 6]" else "🎳 Knocked [$finalRoll / 6] pins"
                EmojiGameMode.BASKETBALL -> if (finalRoll >= 4) "🏀 SWISH! Basket scored [$finalRoll / 5]" else "🏀 Rim bounce out [$finalRoll / 5]"
                EmojiGameMode.FOOTBALL -> if (finalRoll >= 4) "⚽ GOOOAL! Top bins [$finalRoll / 5]" else "⚽ Saved by keeper [$finalRoll / 5]"
            }

            val headline = if (payout > 0) {
                "$resultDescription — WON +${formatCrypto(payout, currency)} ${currency.code} (${multiplier}x)!"
            } else {
                "$resultDescription — No payout (-${formatCrypto(bet, currency)} ${currency.code})."
            }

            settleWagerAndRecord(
                profile = profile,
                currency = currency,
                betAmount = bet,
                payoutAmount = payout,
                gameType = mode.gameTypeKey,
                emojiSymbol = mode.emoji,
                emojiValue = finalRoll.toLong(),
                outcome = outcome,
                botMessageText = headline,
                metadataSummary = "Roll $finalRoll/${mode.maxRoll} • Multiplier ${multiplier}x • $sourceTag"
            )

            _isRollingEmoji.value = false
        }
    }

    fun handleCommandInput(rawInput: String, fromRemoteTelegram: Boolean = false) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return
        val profile = currentProfileOrNull() ?: return
        val currency = CryptoCurrency.fromCode(profile.activeCurrency)

        val parts = trimmed.split("\\s+".toRegex())
        val cmd = parts.first().lowercase(Locale.US)
        val arg1 = parts.getOrNull(1)
        val arg2 = parts.getOrNull(2)

        when (cmd) {
            "/start", "/help" -> {
                viewModelScope.launch {
                    repository.recordMessage(
                        sender = "USER",
                        commandOrText = trimmed,
                        gameType = "NONE",
                        emojiSymbol = "💬",
                        currency = currency.code,
                        outcome = "INFO"
                    )
                    val helpText = "🤖 @TeleVegasCasinoBot Command Menu:\n" +
                        "• /dice [bet] — Roll 🎲 (4-5: 1.8x, 6: 3.0x)\n" +
                        "• /darts [bet] — Throw 🎯 (5: 2.0x, 6 Bullseye: 4.0x)\n" +
                        "• /bowl [bet] — Roll 🎳 (5: 1.9x, 6 Strike: 3.5x)\n" +
                        "• /hoops [bet] — Shoot 🏀 (4: 2.0x, 5 Swish: 3.2x)\n" +
                        "• /goal [bet] — Kick ⚽ (4: 1.9x, 5 Top Bins: 3.0x)\n" +
                        "• /slots [bet] — Spin 🎰 Telegram 1-64 Reel\n" +
                        "• /bj [bet] — Instant Blackjack Hand\n" +
                        "• /roulette [red|black|even|odd|zero] [bet]\n" +
                        "• /balance — View Crypto Vault\n" +
                        "• /faucet — Claim Free Testnet Crypto\n" +
                        "• /seed — View Provably Fair SHA-256 Seed\n" +
                        "• /ai [question] — Ask Gemini AI Pit Boss & Strategy Advisor"
                    repository.recordMessage(
                        sender = "BOT",
                        commandOrText = helpText,
                        gameType = "NONE",
                        emojiSymbol = "🤖",
                        currency = currency.code,
                        outcome = "INFO",
                        metadataSummary = if (fromRemoteTelegram) "Synced via Telegram Bot API" else "Interactive Command List"
                    )
                    if (telegramClient.isTokenConfigured && profile.botChatId.isNotBlank()) {
                        telegramClient.sendCasinoMessage(
                            chatId = profile.botChatId,
                            htmlText = helpText,
                            includeInlineKeyboard = true
                        )
                    }
                }
            }
            "/dice", "🎲" -> playEmojiGame(EmojiGameMode.DICE, arg1?.toDoubleOrNull())
            "/darts", "🎯" -> playEmojiGame(EmojiGameMode.DARTS, arg1?.toDoubleOrNull())
            "/bowl", "🎳" -> playEmojiGame(EmojiGameMode.BOWLING, arg1?.toDoubleOrNull())
            "/hoops", "🏀" -> playEmojiGame(EmojiGameMode.BASKETBALL, arg1?.toDoubleOrNull())
            "/goal", "⚽" -> playEmojiGame(EmojiGameMode.FOOTBALL, arg1?.toDoubleOrNull())
            "/slots", "🎰" -> {
                val bet = arg1?.toDoubleOrNull() ?: _slotsState.value.betAmount
                _slotsState.update { it.copy(betAmount = bet) }
                spinSlots()
            }
            "/bj", "/blackjack" -> {
                val bet = arg1?.toDoubleOrNull() ?: _blackjackState.value.betAmount
                _blackjackState.update { it.copy(betAmount = bet) }
                dealBlackjack()
            }
            "/roulette" -> {
                val betType = when (arg1?.lowercase(Locale.US)) {
                    "black" -> RouletteBetType.BLACK
                    "even" -> RouletteBetType.EVEN
                    "odd" -> RouletteBetType.ODD
                    "zero", "0", "green" -> RouletteBetType.GREEN_ZERO
                    else -> RouletteBetType.RED
                }
                val bet = arg2?.toDoubleOrNull() ?: arg1?.toDoubleOrNull() ?: _rouletteState.value.betAmount
                _rouletteState.update { it.copy(selectedBetType = betType, betAmount = bet) }
                spinRoulette()
            }
            "/balance", "/wallet" -> {
                viewModelScope.launch {
                    repository.recordMessage(
                        sender = "USER",
                        commandOrText = trimmed,
                        gameType = "WALLET",
                        emojiSymbol = "💎",
                        currency = currency.code,
                        outcome = "INFO"
                    )
                    val usdTotal = profile.tonBalance * CryptoCurrency.TON.usdRate +
                        profile.usdtBalance * CryptoCurrency.USDT.usdRate +
                        profile.btcBalance * CryptoCurrency.BTC.usdRate
                    val balText = "🏦 Vault Balances for @${profile.username}:\n" +
                        "💎 TON: ${formatCrypto(profile.tonBalance, CryptoCurrency.TON)}\n" +
                        "💵 USDT: ${formatCrypto(profile.usdtBalance, CryptoCurrency.USDT)}\n" +
                        "₿ BTC: ${formatCrypto(profile.btcBalance, CryptoCurrency.BTC)}\n" +
                        "📊 Est. Portfolio Value: $${String.format(Locale.US, "%,.2f", usdTotal)} USD (VIP Lv.${profile.vipLevel})"
                    repository.recordMessage(
                        sender = "BOT",
                        commandOrText = balText,
                        gameType = "WALLET",
                        emojiSymbol = "🏦",
                        currency = currency.code,
                        outcome = "INFO",
                        metadataSummary = "Games Played: ${profile.gamesPlayed}"
                    )
                    if (telegramClient.isTokenConfigured && profile.botChatId.isNotBlank()) {
                        telegramClient.sendCasinoMessage(chatId = profile.botChatId, htmlText = balText)
                    }
                }
            }
            "/faucet" -> claimCryptoFaucet(currency)
            "/seed" -> {
                viewModelScope.launch {
                    repository.recordMessage(
                        sender = "BOT",
                        commandOrText = "🔐 Provably Fair Verification:\n" +
                            "• Server Seed SHA-256: ${profile.serverSeedHash.take(28)}...\n" +
                            "• Client Seed: ${profile.clientSeed}\n" +
                            "• Active Nonce: #${profile.nonce}",
                        gameType = "NONE",
                        emojiSymbol = "🔐",
                        currency = currency.code,
                        outcome = "INFO",
                        metadataSummary = "Zero-Trust SHA-256 RNG"
                    )
                }
            }
            "/clear" -> {
                viewModelScope.launch {
                    repository.clearChatHistory()
                }
            }
            "/ai", "/ask", "/odds" -> {
                val query = parts.drop(1).joinToString(" ").ifBlank {
                    "Give me a concise VIP strategy tip for Blackjack, European Roulette, and managing my crypto bankroll."
                }
                askAiPitBoss(query, recordToChat = true)
            }
            else -> {
                if (trimmed.startsWith("/")) {
                    viewModelScope.launch {
                        repository.recordMessage(
                            sender = "USER",
                            commandOrText = trimmed,
                            gameType = "NONE",
                            emojiSymbol = "💬",
                            currency = currency.code,
                            outcome = "INFO"
                        )
                        repository.recordMessage(
                            sender = "BOT",
                            commandOrText = "🤖 Unknown command: \"$trimmed\". Try /dice 5, /darts 10, /slots 5, /bj 10, /roulette red 5, /ai [question], or /help!",
                            gameType = "NONE",
                            emojiSymbol = "🤖",
                            currency = currency.code,
                            outcome = "INFO"
                        )
                    }
                } else {
                    // Natural language query -> route to Gemini AI Pit Boss
                    askAiPitBoss(trimmed, recordToChat = true)
                }
            }
        }
    }

    // =========================================================================
    // GEMINI AI PIT BOSS & STRATEGY ADVISOR (FIREBASE AI LOGIC)
    // =========================================================================

    fun dismissAiAdvice() {
        _aiAssistantState.value = AiAssistantState.Idle
    }

    fun analyzeBlackjackHandWithAi() {
        val bj = _blackjackState.value
        val profile = currentProfileOrNull()
        val currency = CryptoCurrency.fromCode(profile?.activeCurrency ?: "TON")
        val prompt = if (bj.playerHand.isNotEmpty() && bj.dealerHand.isNotEmpty()) {
            val playerCards = bj.playerHand.joinToString(", ") { it.display }
            val dealerUp = bj.dealerHand.first().display
            "I am playing Blackjack (3:2 payout, Dealer stands on all 17s). " +
                "My hand: [$playerCards] (Total: ${bj.playerScore}). " +
                "Dealer's visible upcard: [$dealerUp] (Value: ${bj.dealerHand.first().baseValue}). " +
                "My current bet is ${formatCrypto(bj.betAmount, currency)} ${currency.code}. " +
                "Should I HIT, STAND, or DOUBLE DOWN right now? Explain the mathematical basic strategy reason in 2-3 concise sentences."
        } else {
            "Explain the mathematical optimal Blackjack basic strategy for when to Hit, Stand, or Double Down in a 3:2 crypto blackjack game where dealer stands on 17, in 3 concise bullet points."
        }
        askAiPitBoss(prompt, recordToChat = true)
    }

    fun askAiPitBoss(userPrompt: String, recordToChat: Boolean = true) {
        val cleanPrompt = userPrompt.trim()
        if (cleanPrompt.isBlank()) return
        val profile = currentProfileOrNull()
        val currency = CryptoCurrency.fromCode(profile?.activeCurrency ?: "TON")

        viewModelScope.launch {
            _aiAssistantState.value = AiAssistantState.Loading
            if (recordToChat) {
                repository.recordMessage(
                    sender = "USER",
                    commandOrText = "✨ /ai $cleanPrompt",
                    gameType = "NONE",
                    emojiSymbol = "🧠",
                    currency = currency.code,
                    outcome = "INFO",
                    metadataSummary = "Gemini 3.8 Flash Pit Boss Query"
                )
            }

            val contextInstruction = buildString {
                appendLine("You are the VIP AI Pit Boss & Crypto Strategy Advisor for @TeleVegasCasinoBot, a Telegram Crypto Casino.")
                if (profile != null) {
                    appendLine(
                        "Player @${profile.username} (VIP Lv.${profile.vipLevel}) has balances: " +
                            "${formatCrypto(profile.tonBalance, CryptoCurrency.TON)} TON, " +
                            "${formatCrypto(profile.usdtBalance, CryptoCurrency.USDT)} USDT, " +
                            "${formatCrypto(profile.btcBalance, CryptoCurrency.BTC)} BTC. " +
                            "Active currency: ${currency.code}."
                    )
                }
                appendLine("Provide sharp, mathematically accurate, responsible, and concise advice (under 90 words) with casino emojis.")
                appendLine("Player question: $cleanPrompt")
            }

            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    geminiModel.generateContentStream(contextInstruction).collect { chunk ->
                        val part = chunk.text.orEmpty()
                        if (part.isNotEmpty()) {
                            accumulated.append(part)
                            _aiAssistantState.value = AiAssistantState.Streaming(accumulated.toString())
                        }
                    }
                }
                val finalResponse = accumulated.toString().trim().ifBlank {
                    "🎩 Pit Boss: Keep your wagers disciplined (1–3% of your crypto vault per round) and stick to basic strategy!"
                }
                _aiAssistantState.value = AiAssistantState.Success(finalResponse)
                if (recordToChat) {
                    repository.recordMessage(
                        sender = "BOT",
                        commandOrText = "✨ AI Pit Boss:\n$finalResponse",
                        gameType = "NONE",
                        emojiSymbol = "✨",
                        currency = currency.code,
                        outcome = "INFO",
                        metadataSummary = "Powered by Gemini 3.8 Flash"
                    )
                    if (telegramClient.isTokenConfigured && profile != null && profile.botChatId.isNotBlank()) {
                        telegramClient.sendCasinoMessage(
                            chatId = profile.botChatId,
                            htmlText = "<b>✨ @TeleVegasCasinoBot AI Pit Boss</b>\n$finalResponse"
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                val msg = "AI Pit Boss request timed out after 60s. Please try again."
                Log.w(TAG, msg, e)
                _aiAssistantState.value = AiAssistantState.Error(msg)
                showBanner(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Failed to reach Gemini AI Pit Boss"
                Log.e(TAG, "Gemini AI Pit Boss error", e)
                _aiAssistantState.value = AiAssistantState.Error(msg)
                showBanner("AI Pit Boss Error: $msg")
            }
        }
    }

    // =========================================================================
    // 2. BLACKJACK ENGINE (DEAL, HIT, STAND, DOUBLE DOWN)
    // =========================================================================

    fun setBlackjackBet(amount: Double) {
        if (_blackjackState.value.stage == BlackjackStage.PLAYER_TURN) return
        _blackjackState.update { it.copy(betAmount = amount.coerceAtLeast(0.0001)) }
    }

    private fun drawRandomCard(profile: PlayerProfile, salt: String): PlayingCard {
        val ranks = listOf(
            "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6,
            "7" to 7, "8" to 8, "9" to 9, "10" to 10,
            "J" to 10, "Q" to 10, "K" to 10, "A" to 11
        )
        val suits = listOf("♠️", "♥️", "♦️", "♣️")
        val (rIdx, _) = generateProvablyFairInt(profile, ranks.size, "rank_$salt")
        val (sIdx, _) = generateProvablyFairInt(profile, suits.size, "suit_$salt")
        val (rank, baseVal) = ranks[rIdx - 1]
        val suit = suits[sIdx - 1]
        return PlayingCard(rank, suit, baseVal)
    }

    fun dealBlackjack() {
        val currentBj = _blackjackState.value
        if (currentBj.stage == BlackjackStage.PLAYER_TURN) return
        val profile = currentProfileOrNull() ?: return
        val currency = CryptoCurrency.fromCode(profile.activeCurrency)
        val bet = currentBj.betAmount
        if (bet > profile.balanceFor(currency)) {
            showBanner("Insufficient ${currency.code} balance for Blackjack hand!")
            return
        }

        val p1 = drawRandomCard(profile, "p1")
        val d1 = drawRandomCard(profile, "d1")
        val p2 = drawRandomCard(profile, "p2")
        val d2 = drawRandomCard(profile, "d2")

        val playerHand = listOf(p1, p2)
        val dealerHand = listOf(d1, d2)
        val playerScore = BlackjackState.calculateBestScore(playerHand)
        val dealerScore = BlackjackState.calculateBestScore(dealerHand)

        if (playerScore == 21) {
            val isPush = dealerScore == 21
            val payout = if (isPush) bet else bet * 2.5
            val outcome = if (isPush) "PUSH" else "JACKPOT"
            val status = if (isPush) {
                "Both dealt Natural 21! Push — Bet returned."
            } else {
                "🃏 NATURAL BLACKJACK 21! Paid 3:2 (+${formatCrypto(payout, currency)} ${currency.code})!"
            }
            _blackjackState.value = BlackjackState(
                stage = BlackjackStage.SETTLED,
                playerHand = playerHand,
                dealerHand = dealerHand,
                dealerHidden = false,
                betAmount = bet,
                payoutAmount = payout,
                statusText = status,
                outcome = outcome
            )
            viewModelScope.launch {
                settleWagerAndRecord(
                    profile = profile,
                    currency = currency,
                    betAmount = bet,
                    payoutAmount = payout,
                    gameType = "BLACKJACK",
                    emojiSymbol = "🃏",
                    emojiValue = 21L,
                    outcome = outcome,
                    botMessageText = "🃏 Blackjack Table: ${playerHand.joinToString(" ") { it.display }} (21) vs Dealer ${dealerHand.joinToString(" ") { it.display }} ($dealerScore) — $status",
                    metadataSummary = "Natural Blackjack 21 (3:2)"
                )
            }
        } else {
            _blackjackState.value = BlackjackState(
                stage = BlackjackStage.PLAYER_TURN,
                playerHand = playerHand,
                dealerHand = dealerHand,
                dealerHidden = true,
                betAmount = bet,
                payoutAmount = 0.0,
                statusText = "Your Hand: $playerScore vs Dealer ${d1.display}. Hit, Stand, or Double Down!",
                outcome = "ACTIVE"
            )
        }
    }

    fun hitBlackjack() {
        val state = _blackjackState.value
        if (state.stage != BlackjackStage.PLAYER_TURN) return
        val profile = currentProfileOrNull() ?: return
        val currency = CryptoCurrency.fromCode(profile.activeCurrency)

        val newCard = drawRandomCard(profile, "hit_${state.playerHand.size}")
        val updatedHand = state.playerHand + newCard
        val newScore = BlackjackState.calculateBestScore(updatedHand)

        if (newScore > 21) {
            val status = "💥 BUST ($newScore)! Dealer wins (-${formatCrypto(state.betAmount, currency)} ${currency.code})."
            _blackjackState.value = state.copy(
                stage = BlackjackStage.SETTLED,
                playerHand = updatedHand,
                dealerHidden = false,
                payoutAmount = 0.0,
                statusText = status,
                outcome = "LOSS"
            )
            viewModelScope.launch {
                settleWagerAndRecord(
                    profile = profile,
                    currency = currency,
                    betAmount = state.betAmount,
                    payoutAmount = 0.0,
                    gameType = "BLACKJACK",
                    emojiSymbol = "🃏",
                    emojiValue = newScore.toLong().coerceAtMost(64L),
                    outcome = "LOSS",
                    botMessageText = "🃏 Blackjack Bust: ${updatedHand.joinToString(" ") { it.display }} ($newScore) — $status",
                    metadataSummary = "Player Bust ($newScore)"
                )
            }
        } else if (newScore == 21) {
            _blackjackState.value = state.copy(playerHand = updatedHand)
            standBlackjack()
        } else {
            _blackjackState.value = state.copy(
                playerHand = updatedHand,
                statusText = "Drew ${newCard.display} • Hand Total: $newScore. Hit or Stand?"
            )
        }
    }

    fun doubleDownBlackjack() {
        val state = _blackjackState.value
        if (state.stage != BlackjackStage.PLAYER_TURN || state.playerHand.size != 2) return
        val profile = currentProfileOrNull() ?: return
        val currency = CryptoCurrency.fromCode(profile.activeCurrency)
        val doubledBet = state.betAmount * 2.0
        if (doubledBet > profile.balanceFor(currency)) {
            showBanner("Insufficient ${currency.code} balance to Double Down!")
            return
        }
        val newCard = drawRandomCard(profile, "double")
        val updatedHand = state.playerHand + newCard
        _blackjackState.value = state.copy(
            playerHand = updatedHand,
            betAmount = doubledBet
        )
        val newScore = BlackjackState.calculateBestScore(updatedHand)
        if (newScore > 21) {
            val status = "💥 Double Down BUST ($newScore)! (-${formatCrypto(doubledBet, currency)} ${currency.code})"
            _blackjackState.update {
                it.copy(
                    stage = BlackjackStage.SETTLED,
                    dealerHidden = false,
                    payoutAmount = 0.0,
                    statusText = status,
                    outcome = "LOSS"
                )
            }
            viewModelScope.launch {
                settleWagerAndRecord(
                    profile = profile,
                    currency = currency,
                    betAmount = doubledBet,
                    payoutAmount = 0.0,
                    gameType = "BLACKJACK",
                    emojiSymbol = "🃏",
                    emojiValue = newScore.toLong().coerceAtMost(64L),
                    outcome = "LOSS",
                    botMessageText = "🃏 Blackjack Double Down: ${updatedHand.joinToString(" ") { it.display }} ($newScore) — $status",
                    metadataSummary = "Double Down Bust ($newScore)"
                )
            }
        } else {
            standBlackjack()
        }
    }

    fun standBlackjack() {
        val state = _blackjackState.value
        if (state.stage != BlackjackStage.PLAYER_TURN) return
        val profile = currentProfileOrNull() ?: return
        val currency = CryptoCurrency.fromCode(profile.activeCurrency)

        var dealerHand = state.dealerHand
        var step = 1
        while (BlackjackState.calculateBestScore(dealerHand) < 17) {
            dealerHand = dealerHand + drawRandomCard(profile, "dealer_$step")
            step++
        }

        val playerScore = BlackjackState.calculateBestScore(state.playerHand)
        val dealerScore = BlackjackState.calculateBestScore(dealerHand)

        val (payout, outcome, status) = when {
            dealerScore > 21 -> Triple(
                state.betAmount * 2.0,
                "WIN",
                "🎉 Dealer Busts ($dealerScore)! You win +${formatCrypto(state.betAmount * 2.0, currency)} ${currency.code}!"
            )
            playerScore > dealerScore -> Triple(
                state.betAmount * 2.0,
                "WIN",
                "🏆 You beat Dealer ($playerScore vs $dealerScore)! Won +${formatCrypto(state.betAmount * 2.0, currency)} ${currency.code}!"
            )
            playerScore == dealerScore -> Triple(
                state.betAmount,
                "PUSH",
                "🤝 Push ($playerScore vs $dealerScore)! Bet returned."
            )
            else -> Triple(
                0.0,
                "LOSS",
                "Dealer wins ($dealerScore vs $playerScore). (-${formatCrypto(state.betAmount, currency)} ${currency.code})"
            )
        }

        _blackjackState.value = state.copy(
            stage = BlackjackStage.SETTLED,
            dealerHand = dealerHand,
            dealerHidden = false,
            payoutAmount = payout,
            statusText = status,
            outcome = outcome
        )

        viewModelScope.launch {
            settleWagerAndRecord(
                profile = profile,
                currency = currency,
                betAmount = state.betAmount,
                payoutAmount = payout,
                gameType = "BLACKJACK",
                emojiSymbol = "🃏",
                emojiValue = playerScore.toLong().coerceIn(0L, 64L),
                outcome = outcome,
                botMessageText = "🃏 Blackjack Settlement: Player ${state.playerHand.joinToString(" ") { it.display }} ($playerScore) vs Dealer ${dealerHand.joinToString(" ") { it.display }} ($dealerScore) — $status",
                metadataSummary = "Player $playerScore vs Dealer $dealerScore"
            )
        }
    }

    // =========================================================================
    // 3. CYBER SLOTS & TELEGRAM 1-64 SLOT MACHINE
    // =========================================================================

    fun setSlotsBet(amount: Double) {
        if (_slotsState.value.isSpinning) return
        _slotsState.update { it.copy(betAmount = amount.coerceAtLeast(0.0001)) }
    }

    fun spinSlots() {
        val current = _slotsState.value
        if (current.isSpinning) return
        val profile = currentProfileOrNull() ?: return
        val currency = CryptoCurrency.fromCode(profile.activeCurrency)
        val bet = current.betAmount
        if (bet > profile.balanceFor(currency)) {
            showBanner("Insufficient ${currency.code} balance to spin Slots!")
            return
        }

        viewModelScope.launch {
            _slotsState.update { it.copy(isSpinning = true, winningLines = emptyList(), statusText = "Spinning Cyber Reels & Telegram 🎰...") }

            // If live Telegram Bot API is configured with a chat_id, send real 🎰 dice animation!
            val liveSlotDice = if (telegramClient.isTokenConfigured && profile.botChatId.isNotBlank()) {
                telegramClient.sendEmojiDiceRoll(chatId = profile.botChatId, emoji = "🎰").getOrNull()
            } else {
                null
            }

            delay(450)

            val (provableSlotVal, roundHash) = generateProvablyFairInt(profile, 64, "tg_slot")
            val tgSlotVal = liveSlotDice?.value ?: provableSlotVal
            val decodedCenterRow = TelegramBotApiService.decodeTelegramSlotReels(tgSlotVal)

            val symbols = SlotSymbol.entries
            val newGrid = List(3) { r ->
                if (r == 1) {
                    decodedCenterRow
                } else {
                    List(3) { c ->
                        val (idx, _) = generateProvablyFairInt(profile, symbols.size, "slot_${r}_$c")
                        symbols[idx - 1]
                    }
                }
            }

            // Evaluate 5 paylines: 3 horizontal rows + 2 diagonals
            val paylines = listOf(
                listOf(newGrid[0][0], newGrid[0][1], newGrid[0][2]),
                listOf(newGrid[1][0], newGrid[1][1], newGrid[1][2]),
                listOf(newGrid[2][0], newGrid[2][1], newGrid[2][2]),
                listOf(newGrid[0][0], newGrid[1][1], newGrid[2][2]),
                listOf(newGrid[2][0], newGrid[1][1], newGrid[0][2])
            )

            var totalMultiplier = 0.0
            val matchedLines = mutableListOf<Int>()

            paylines.forEachIndexed { index, line ->
                if (line[0] == line[1] && line[1] == line[2]) {
                    totalMultiplier += line[0].tripleMultiplier
                    matchedLines.add(index)
                } else if (index == 1 && (line[0] == line[1] || line[1] == line[2])) {
                    val matchedSym = if (line[0] == line[1]) line[0] else line[1]
                    totalMultiplier += matchedSym.doubleMultiplier
                    matchedLines.add(index)
                }
            }

            val payout = bet * totalMultiplier
            val outcome = when {
                totalMultiplier >= 8.0 -> "JACKPOT"
                totalMultiplier > 0.0 -> "WIN"
                else -> "LOSS"
            }

            val centerReelStr = newGrid[1].joinToString(" | ") { it.emoji }
            val status = if (payout > 0) {
                "🎰 WIN! [$centerReelStr] (${String.format(Locale.US, "%.1fx", totalMultiplier)}) — Paid +${formatCrypto(payout, currency)} ${currency.code}!"
            } else {
                "🎰 Reels: [$centerReelStr] — Try again! (Telegram 🎰 Roll #$tgSlotVal/64)"
            }

            _slotsState.value = SlotsState(
                grid = newGrid,
                telegramSlotValue = tgSlotVal,
                betAmount = bet,
                lastPayout = payout,
                winningLines = matchedLines,
                statusText = status,
                isSpinning = false
            )

            settleWagerAndRecord(
                profile = profile,
                currency = currency,
                betAmount = bet,
                payoutAmount = payout,
                gameType = "SLOTS",
                emojiSymbol = "🎰",
                emojiValue = tgSlotVal.toLong(),
                outcome = outcome,
                botMessageText = status,
                metadataSummary = "5-Line Slots • Matched ${matchedLines.size} line(s) • Roll #$tgSlotVal/64 • PF #$roundHash"
            )
        }
    }

    // =========================================================================
    // 4. EUROPEAN CRYPTO ROULETTE ENGINE (0-36)
    // =========================================================================

    fun selectRouletteBetType(betType: RouletteBetType) {
        if (_rouletteState.value.isSpinning) return
        _rouletteState.update { it.copy(selectedBetType = betType) }
    }

    fun selectRouletteExactNumber(number: Int) {
        if (_rouletteState.value.isSpinning) return
        _rouletteState.update {
            it.copy(
                selectedNumber = number.coerceIn(0, 36),
                selectedBetType = if (number == 0) RouletteBetType.GREEN_ZERO else RouletteBetType.STRAIGHT_NUMBER
            )
        }
    }

    fun setRouletteBet(amount: Double) {
        if (_rouletteState.value.isSpinning) return
        _rouletteState.update { it.copy(betAmount = amount.coerceAtLeast(0.0001)) }
    }

    fun spinRoulette() {
        val current = _rouletteState.value
        if (current.isSpinning) return
        val profile = currentProfileOrNull() ?: return
        val currency = CryptoCurrency.fromCode(profile.activeCurrency)
        val bet = current.betAmount
        if (bet > profile.balanceFor(currency)) {
            showBanner("Insufficient ${currency.code} balance for Roulette spin!")
            return
        }

        viewModelScope.launch {
            _rouletteState.update { it.copy(isSpinning = true, statusText = "Spinning European Roulette Wheel...") }
            delay(500)

            val (rawRoll, roundHash) = generateProvablyFairInt(profile, 37, "roulette")
            val pocket = rawRoll - 1 // 0..36
            val isRed = RouletteState.RED_NUMBERS.contains(pocket)
            val colorEmoji = RouletteState.pocketEmoji(pocket)
            val colorName = RouletteState.pocketColorName(pocket)

            val won = when (current.selectedBetType) {
                RouletteBetType.RED -> pocket != 0 && isRed
                RouletteBetType.BLACK -> pocket != 0 && !isRed
                RouletteBetType.EVEN -> pocket != 0 && pocket % 2 == 0
                RouletteBetType.ODD -> pocket != 0 && pocket % 2 != 0
                RouletteBetType.LOW_1_18 -> pocket in 1..18
                RouletteBetType.HIGH_19_36 -> pocket in 19..36
                RouletteBetType.DOZEN_1 -> pocket in 1..12
                RouletteBetType.DOZEN_2 -> pocket in 13..24
                RouletteBetType.DOZEN_3 -> pocket in 25..36
                RouletteBetType.GREEN_ZERO -> pocket == 0
                RouletteBetType.STRAIGHT_NUMBER -> pocket == current.selectedNumber
            }

            val multiplier = if (won) current.selectedBetType.multiplier else 0.0
            val payout = bet * multiplier
            val outcome = when {
                multiplier >= 36.0 -> "JACKPOT"
                won -> "WIN"
                else -> "LOSS"
            }

            val betDesc = if (current.selectedBetType == RouletteBetType.STRAIGHT_NUMBER) {
                "Straight #${current.selectedNumber}"
            } else {
                current.selectedBetType.label
            }

            val status = if (won) {
                "🎡 Ball landed on $colorEmoji $pocket ($colorName)! Your $betDesc bet WON +${formatCrypto(payout, currency)} ${currency.code} (${multiplier}x)!"
            } else {
                "🎡 Ball landed on $colorEmoji $pocket ($colorName). Your $betDesc bet lost (-${formatCrypto(bet, currency)} ${currency.code})."
            }

            val updatedRecent = (listOf(pocket) + current.recentPockets).take(10)
            _rouletteState.value = current.copy(
                lastPocket = pocket,
                lastPayout = payout,
                recentPockets = updatedRecent,
                statusText = status,
                isSpinning = false
            )

            settleWagerAndRecord(
                profile = profile,
                currency = currency,
                betAmount = bet,
                payoutAmount = payout,
                gameType = "ROULETTE",
                emojiSymbol = colorEmoji,
                emojiValue = pocket.toLong(),
                outcome = outcome,
                botMessageText = status,
                metadataSummary = "Pocket $pocket ($colorName) • Bet: $betDesc • PF #$roundHash"
            )
        }
    }

    // =========================================================================
    // 5. CRYPTO WALLET LEDGER (FAUCET, DEPOSIT, WITHDRAW, SWAP)
    // =========================================================================

    fun claimCryptoFaucet(currency: CryptoCurrency) {
        val profile = currentProfileOrNull() ?: return
        val amount = currency.faucetAmount
        val updatedProfile = when (currency) {
            CryptoCurrency.TON -> profile.copy(tonBalance = profile.tonBalance + amount)
            CryptoCurrency.USDT -> profile.copy(usdtBalance = profile.usdtBalance + amount)
            CryptoCurrency.BTC -> profile.copy(btcBalance = profile.btcBalance + amount)
        }
        viewModelScope.launch {
            repository.updateProfile(updatedProfile)
            val txHash = "0x${CasinoRepository.sha256Hex("faucet_${currency.code}_${System.currentTimeMillis()}").take(32)}"
            repository.recordTransaction(
                txType = "FAUCET",
                currency = currency.code,
                amount = amount,
                txHash = txHash,
                addressOrNote = "TeleVegas Instant Crypto Faucet (+${formatCrypto(amount, currency)} ${currency.code})"
            )
            repository.recordMessage(
                sender = "BOT",
                commandOrText = "🚰 Faucet Claimed: +${formatCrypto(amount, currency)} ${currency.code} credited to your wallet!",
                gameType = "WALLET",
                emojiSymbol = currency.symbol,
                currency = currency.code,
                outcome = "WIN",
                metadataSummary = "Tx $txHash"
            )
            showBanner("Credited +${formatCrypto(amount, currency)} ${currency.code} from Faucet!")
        }
    }

    fun simulateCryptoDeposit(currency: CryptoCurrency, amount: Double) {
        if (amount <= 0) return
        val profile = currentProfileOrNull() ?: return
        val updatedProfile = when (currency) {
            CryptoCurrency.TON -> profile.copy(tonBalance = profile.tonBalance + amount)
            CryptoCurrency.USDT -> profile.copy(usdtBalance = profile.usdtBalance + amount)
            CryptoCurrency.BTC -> profile.copy(btcBalance = profile.btcBalance + amount)
        }
        viewModelScope.launch {
            repository.updateProfile(updatedProfile)
            val txHash = "0x${CasinoRepository.sha256Hex("dep_${currency.code}_${System.nanoTime()}").take(32)}"
            repository.recordTransaction(
                txType = "DEPOSIT",
                currency = currency.code,
                amount = amount,
                txHash = txHash,
                addressOrNote = "On-Chain Deposit via ${currency.networkName}"
            )
            showBanner("Confirmed Deposit: +${formatCrypto(amount, currency)} ${currency.code}")
        }
    }

    fun withdrawCrypto(currency: CryptoCurrency, amount: Double, destinationAddress: String) {
        val profile = currentProfileOrNull() ?: return
        if (amount <= 0 || amount > profile.balanceFor(currency)) {
            showBanner("Invalid withdrawal amount or insufficient ${currency.code} balance.")
            return
        }
        val cleanAddress = destinationAddress.trim().ifBlank { currency.sampleDepositAddress }
        val updatedProfile = when (currency) {
            CryptoCurrency.TON -> profile.copy(tonBalance = (profile.tonBalance - amount).coerceAtLeast(0.0))
            CryptoCurrency.USDT -> profile.copy(usdtBalance = (profile.usdtBalance - amount).coerceAtLeast(0.0))
            CryptoCurrency.BTC -> profile.copy(btcBalance = (profile.btcBalance - amount).coerceAtLeast(0.0))
        }
        viewModelScope.launch {
            repository.updateProfile(updatedProfile)
            val txHash = "0x${CasinoRepository.sha256Hex("wd_${cleanAddress}_${System.nanoTime()}").take(32)}"
            repository.recordTransaction(
                txType = "WITHDRAW",
                currency = currency.code,
                amount = amount,
                txHash = txHash,
                addressOrNote = "Withdraw to ${cleanAddress.take(24)}..."
            )
            showBanner("Broadcasted Withdrawal of ${formatCrypto(amount, currency)} ${currency.code}!")
        }
    }

    fun swapCrypto(fromCurrency: CryptoCurrency, toCurrency: CryptoCurrency, fromAmount: Double) {
        if (fromCurrency == toCurrency || fromAmount <= 0) return
        val profile = currentProfileOrNull() ?: return
        if (fromAmount > profile.balanceFor(fromCurrency)) {
            showBanner("Insufficient ${fromCurrency.code} balance for swap.")
            return
        }
        val usdValue = fromAmount * fromCurrency.usdRate
        val toAmount = usdValue / toCurrency.usdRate

        var ton = profile.tonBalance
        var usdt = profile.usdtBalance
        var btc = profile.btcBalance

        when (fromCurrency) {
            CryptoCurrency.TON -> ton -= fromAmount
            CryptoCurrency.USDT -> usdt -= fromAmount
            CryptoCurrency.BTC -> btc -= fromAmount
        }
        when (toCurrency) {
            CryptoCurrency.TON -> ton += toAmount
            CryptoCurrency.USDT -> usdt += toAmount
            CryptoCurrency.BTC -> btc += toAmount
        }

        viewModelScope.launch {
            repository.updateProfile(
                profile.copy(
                    tonBalance = ton.coerceAtLeast(0.0),
                    usdtBalance = usdt.coerceAtLeast(0.0),
                    btcBalance = btc.coerceAtLeast(0.0)
                )
            )
            val txHash = "0x${CasinoRepository.sha256Hex("swap_${fromCurrency.code}_${toCurrency.code}_${System.nanoTime()}").take(32)}"
            repository.recordTransaction(
                txType = "SWAP",
                currency = toCurrency.code,
                amount = toAmount,
                txHash = txHash,
                addressOrNote = "Swapped ${formatCrypto(fromAmount, fromCurrency)} ${fromCurrency.code} → ${formatCrypto(toAmount, toCurrency)} ${toCurrency.code}"
            )
            showBanner("Swapped ${formatCrypto(fromAmount, fromCurrency)} ${fromCurrency.code} → ${formatCrypto(toAmount, toCurrency)} ${toCurrency.code}!")
        }
    }

    // =========================================================================
    // SETTLEMENT HELPER (FIRESTORE + LIVE TELEGRAM BOT BROADCAST)
    // =========================================================================

    private suspend fun settleWagerAndRecord(
        profile: PlayerProfile,
        currency: CryptoCurrency,
        betAmount: Double,
        payoutAmount: Double,
        gameType: String,
        emojiSymbol: String,
        emojiValue: Long,
        outcome: String,
        botMessageText: String,
        metadataSummary: String
    ) {
        val netDelta = payoutAmount - betAmount
        val newTon = if (currency == CryptoCurrency.TON) (profile.tonBalance + netDelta).coerceAtLeast(0.0) else profile.tonBalance
        val newUsdt = if (currency == CryptoCurrency.USDT) (profile.usdtBalance + netDelta).coerceAtLeast(0.0) else profile.usdtBalance
        val newBtc = if (currency == CryptoCurrency.BTC) (profile.btcBalance + netDelta).coerceAtLeast(0.0) else profile.btcBalance

        val betUsd = betAmount * currency.usdRate
        val winUsd = payoutAmount * currency.usdRate
        val newTotalWagered = profile.totalWagered + betUsd
        val newTotalWon = profile.totalWon + winUsd
        val newGamesPlayed = profile.gamesPlayed + 1L
        val newVip = (1L + (newTotalWagered / 500.0).toLong()).coerceIn(1L, 10L)

        val updatedProfile = profile.copy(
            tonBalance = newTon,
            usdtBalance = newUsdt,
            btcBalance = newBtc,
            totalWagered = newTotalWagered,
            totalWon = newTotalWon,
            gamesPlayed = newGamesPlayed,
            vipLevel = newVip,
            nonce = profile.nonce + 1L
        )

        repository.updateProfile(updatedProfile)

        repository.recordMessage(
            sender = "BOT",
            commandOrText = botMessageText,
            gameType = gameType,
            emojiSymbol = emojiSymbol,
            emojiValue = emojiValue,
            betAmount = betAmount,
            payoutAmount = payoutAmount,
            currency = currency.code,
            outcome = outcome,
            metadataSummary = metadataSummary
        )

        val txHash = "0x${CasinoRepository.sha256Hex("${gameType}_${profile.nonce}_${System.nanoTime()}").take(32)}"
        repository.recordTransaction(
            txType = if (payoutAmount >= betAmount && payoutAmount > 0.0) "BET_WIN" else "BET_LOSS",
            currency = currency.code,
            amount = if (payoutAmount > 0.0) payoutAmount else betAmount,
            txHash = txHash,
            addressOrNote = "$gameType • $metadataSummary"
        )

        // Broadcast settlement message to live Telegram chat via TelegramBotClient
        if (telegramClient.isTokenConfigured && profile.botChatId.isNotBlank()) {
            telegramClient.sendCasinoMessage(
                chatId = profile.botChatId,
                htmlText = "<b>@TeleVegasCasinoBot</b>\n$botMessageText\n<i>$metadataSummary</i>"
            )
        }
    }

    companion object {
        private const val TAG = "CasinoViewModel"

        fun formatCrypto(amount: Double, currency: CryptoCurrency): String {
            return when (currency) {
                CryptoCurrency.BTC -> String.format(Locale.US, "%.4f", amount)
                else -> String.format(Locale.US, "%.2f", amount)
            }
        }
    }
}
