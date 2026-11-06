package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "TelePlayer",
    val tonBalance: Double = 250.0,
    val usdtBalance: Double = 1000.0,
    val btcBalance: Double = 0.05,
    val activeCurrency: String = "TON",
    val totalWagered: Double = 0.0,
    val totalWon: Double = 0.0,
    val gamesPlayed: Long = 0L,
    val vipLevel: Long = 1L,
    val botChatId: String = "",
    val serverSeedHash: String = "8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4",
    val clientSeed: String = "televegas_provably_fair",
    val nonce: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun balanceFor(currency: CryptoCurrency): Double = when (currency) {
        CryptoCurrency.TON -> tonBalance
        CryptoCurrency.USDT -> usdtBalance
        CryptoCurrency.BTC -> btcBalance
    }
}

@IgnoreExtraProperties
data class BotChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sender: String = "BOT",
    val commandOrText: String = "",
    val gameType: String = "NONE",
    val emojiSymbol: String = "🎰",
    val emojiValue: Long = 0L,
    val betAmount: Double = 0.0,
    val payoutAmount: Double = 0.0,
    val currency: String = "TON",
    val outcome: String = "INFO",
    val metadataSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CryptoTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val txType: String = "DEPOSIT",
    val currency: String = "TON",
    val amount: Double = 0.0,
    val txHash: String = "",
    val addressOrNote: String = "",
    val status: String = "CONFIRMED",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class CryptoCurrency(
    val code: String,
    val displayName: String,
    val symbol: String,
    val usdRate: Double,
    val defaultQuickBets: List<Double>,
    val faucetAmount: Double,
    val networkName: String,
    val sampleDepositAddress: String
) {
    TON(
        code = "TON",
        displayName = "Toncoin",
        symbol = "💎",
        usdRate = 5.40,
        defaultQuickBets = listOf(1.0, 5.0, 10.0, 25.0, 50.0),
        faucetAmount = 50.0,
        networkName = "TON Mainnet (The Open Network)",
        sampleDepositAddress = "UQBb8f9vK2mN4pQ7rS1tU6vW9xY3zA5bC8dE0fG2hI4jK6lM"
    ),
    USDT(
        code = "USDT",
        displayName = "Tether Jetton",
        symbol = "💵",
        usdRate = 1.00,
        defaultQuickBets = listOf(5.0, 10.0, 25.0, 100.0, 250.0),
        faucetAmount = 200.0,
        networkName = "TON / TRC-20 USDT",
        sampleDepositAddress = "TQn9Y2khEsLJW1ChVWFMSMeRDow5KcbLSE"
    ),
    BTC(
        code = "BTC",
        displayName = "Bitcoin",
        symbol = "₿",
        usdRate = 88500.0,
        defaultQuickBets = listOf(0.0005, 0.001, 0.0025, 0.005, 0.01),
        faucetAmount = 0.005,
        networkName = "Bitcoin Native SegWit",
        sampleDepositAddress = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh"
    );

    companion object {
        fun fromCode(code: String): CryptoCurrency =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: TON
    }
}

enum class EmojiGameMode(
    val gameTypeKey: String,
    val emoji: String,
    val title: String,
    val command: String,
    val maxRoll: Int,
    val winThresholdDescription: String,
    val multiplierText: String
) {
    DICE(
        gameTypeKey = "EMOJI_DICE",
        emoji = "🎲",
        title = "High Roller Dice",
        command = "/dice",
        maxRoll = 6,
        winThresholdDescription = "Roll 4, 5 (1.8x) or 6 (3.0x)",
        multiplierText = "Up to 3.0x"
    ),
    DARTS(
        gameTypeKey = "EMOJI_DARTS",
        emoji = "🎯",
        title = "Bullseye Darts",
        command = "/darts",
        maxRoll = 6,
        winThresholdDescription = "Inner Ring 5 (2.0x) or Bullseye 6 (4.0x)",
        multiplierText = "Up to 4.0x"
    ),
    BOWLING(
        gameTypeKey = "EMOJI_BOWLING",
        emoji = "🎳",
        title = "Neon Bowling Strike",
        command = "/bowl",
        maxRoll = 6,
        winThresholdDescription = "Spare 5 pins (1.9x) or Strike 6 (3.5x)",
        multiplierText = "Up to 3.5x"
    ),
    BASKETBALL(
        gameTypeKey = "EMOJI_BASKETBALL",
        emoji = "🏀",
        title = "Street Hoops",
        command = "/hoops",
        maxRoll = 5,
        winThresholdDescription = "Rim-In 4 (2.0x) or Clean Swish 5 (3.2x)",
        multiplierText = "Up to 3.2x"
    ),
    FOOTBALL(
        gameTypeKey = "EMOJI_FOOTBALL",
        emoji = "⚽",
        title = "Penalty Kick",
        command = "/goal",
        maxRoll = 5,
        winThresholdDescription = "Goal 4 (1.9x) or Top-Corner 5 (3.0x)",
        multiplierText = "Up to 3.0x"
    )
}

data class PlayingCard(
    val rank: String,
    val suit: String,
    val baseValue: Int
) {
    val display: String get() = "$rank$suit"
    val isRed: Boolean get() = suit == "♥️" || suit == "♦️"
}

enum class BlackjackStage {
    IDLE,
    PLAYER_TURN,
    SETTLED
}

data class BlackjackState(
    val stage: BlackjackStage = BlackjackStage.IDLE,
    val playerHand: List<PlayingCard> = emptyList(),
    val dealerHand: List<PlayingCard> = emptyList(),
    val dealerHidden: Boolean = true,
    val betAmount: Double = 5.0,
    val payoutAmount: Double = 0.0,
    val statusText: String = "Place your crypto bet & tap Deal to start a provably fair hand.",
    val outcome: String = "INFO"
) {
    val playerScore: Int get() = calculateBestScore(playerHand)
    val dealerVisibleScore: Int
        get() = if (dealerHidden && dealerHand.isNotEmpty()) {
            calculateBestScore(listOf(dealerHand.first()))
        } else {
            calculateBestScore(dealerHand)
        }

    companion object {
        fun calculateBestScore(hand: List<PlayingCard>): Int {
            var total = hand.sumOf { it.baseValue }
            var aces = hand.count { it.rank == "A" }
            while (total > 21 && aces > 0) {
                total -= 10
                aces--
            }
            return total
        }
    }
}

enum class SlotSymbol(
    val emoji: String,
    val label: String,
    val tripleMultiplier: Double,
    val doubleMultiplier: Double
) {
    SEVEN("7️⃣", "Lucky 7", 15.0, 2.5),
    DIAMOND("💎", "TON Gem", 10.0, 2.0),
    BAR("🎰", "Jackpot Bar", 8.0, 1.8),
    BELL("🔔", "Golden Bell", 5.0, 1.5),
    CHERRY("🍒", "Neon Cherry", 3.5, 1.2),
    LEMON("🍋", "Cyber Lemon", 2.5, 1.1)
}

data class SlotsState(
    val grid: List<List<SlotSymbol>> = listOf(
        listOf(SlotSymbol.SEVEN, SlotSymbol.DIAMOND, SlotSymbol.SEVEN),
        listOf(SlotSymbol.SEVEN, SlotSymbol.SEVEN, SlotSymbol.SEVEN),
        listOf(SlotSymbol.BAR, SlotSymbol.CHERRY, SlotSymbol.BELL)
    ),
    val telegramSlotValue: Int = 64,
    val betAmount: Double = 5.0,
    val lastPayout: Double = 0.0,
    val winningLines: List<Int> = emptyList(),
    val statusText: String = "Spin the 5-Payline Cyber Reels or send a 🎰 Telegram Slot Roll!",
    val isSpinning: Boolean = false
)

enum class RouletteBetType(
    val label: String,
    val multiplier: Double,
    val description: String
) {
    RED("Red 🔴", 2.0, "18 Red pockets (1:1)"),
    BLACK("Black ⚫", 2.0, "18 Black pockets (1:1)"),
    EVEN("Even", 2.0, "Even numbers 2-36 (1:1)"),
    ODD("Odd", 2.0, "Odd numbers 1-35 (1:1)"),
    LOW_1_18("1 to 18", 2.0, "Low half 1-18 (1:1)"),
    HIGH_19_36("19 to 36", 2.0, "High half 19-36 (1:1)"),
    DOZEN_1("1st 12", 3.0, "Numbers 1-12 (2:1)"),
    DOZEN_2("2nd 12", 3.0, "Numbers 13-24 (2:1)"),
    DOZEN_3("3rd 12", 3.0, "Numbers 25-36 (2:1)"),
    GREEN_ZERO("Zero 🟢 0", 36.0, "Single Zero Straight (35:1)"),
    STRAIGHT_NUMBER("Straight #", 36.0, "Exact pocket number (35:1)")
}

data class RouletteState(
    val selectedBetType: RouletteBetType = RouletteBetType.RED,
    val selectedNumber: Int = 7,
    val betAmount: Double = 5.0,
    val lastPocket: Int = 7,
    val lastPayout: Double = 0.0,
    val recentPockets: List<Int> = listOf(7, 0, 23, 14, 32, 19, 4, 21),
    val statusText: String = "Select a board bet or exact pocket and spin the European wheel.",
    val isSpinning: Boolean = false
) {
    companion object {
        val RED_NUMBERS = setOf(
            1, 3, 5, 7, 9, 12, 14, 16, 18,
            19, 21, 23, 25, 27, 30, 32, 34, 36
        )

        fun pocketColorName(pocket: Int): String = when {
            pocket == 0 -> "GREEN"
            RED_NUMBERS.contains(pocket) -> "RED"
            else -> "BLACK"
        }

        fun pocketEmoji(pocket: Int): String = when {
            pocket == 0 -> "🟢"
            RED_NUMBERS.contains(pocket) -> "🔴"
            else -> "⚫"
        }
    }
}
