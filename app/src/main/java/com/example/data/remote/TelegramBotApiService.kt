package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.SlotSymbol
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class TelegramResponse<T>(
    @param:Json(name = "ok") val ok: Boolean = false,
    @param:Json(name = "result") val result: T? = null,
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "error_code") val errorCode: Int? = null
)

@JsonClass(generateAdapter = true)
data class TelegramBotUser(
    @param:Json(name = "id") val id: Long = 0L,
    @param:Json(name = "is_bot") val isBot: Boolean = true,
    @param:Json(name = "first_name") val firstName: String = "",
    @param:Json(name = "username") val username: String? = null
)

@JsonClass(generateAdapter = true)
data class TelegramDice(
    @param:Json(name = "emoji") val emoji: String = "🎲",
    @param:Json(name = "value") val value: Int = 1
)

@JsonClass(generateAdapter = true)
data class TelegramChat(
    @param:Json(name = "id") val id: Long = 0L,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "username") val username: String? = null,
    @param:Json(name = "first_name") val firstName: String? = null
)

@JsonClass(generateAdapter = true)
data class TelegramMessageDto(
    @param:Json(name = "message_id") val messageId: Long = 0L,
    @param:Json(name = "text") val text: String? = null,
    @param:Json(name = "dice") val dice: TelegramDice? = null,
    @param:Json(name = "chat") val chat: TelegramChat? = null
)

@JsonClass(generateAdapter = true)
data class TelegramUpdateDto(
    @param:Json(name = "update_id") val updateId: Long = 0L,
    @param:Json(name = "message") val message: TelegramMessageDto? = null
)

@JsonClass(generateAdapter = true)
data class InlineKeyboardButtonDto(
    @param:Json(name = "text") val text: String,
    @param:Json(name = "callback_data") val callbackData: String
)

@JsonClass(generateAdapter = true)
data class InlineKeyboardMarkupDto(
    @param:Json(name = "inline_keyboard") val inlineKeyboard: List<List<InlineKeyboardButtonDto>>
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @param:Json(name = "chat_id") val chatId: String,
    @param:Json(name = "text") val text: String,
    @param:Json(name = "parse_mode") val parseMode: String = "HTML",
    @param:Json(name = "disable_web_page_preview") val disableWebPagePreview: Boolean = true,
    @param:Json(name = "reply_markup") val replyMarkup: InlineKeyboardMarkupDto? = null
)

@JsonClass(generateAdapter = true)
data class SendDiceRequest(
    @param:Json(name = "chat_id") val chatId: String,
    @param:Json(name = "emoji") val emoji: String
)

@JsonClass(generateAdapter = true)
data class BotCommandDto(
    @param:Json(name = "command") val command: String,
    @param:Json(name = "description") val description: String
)

@JsonClass(generateAdapter = true)
data class SetMyCommandsRequest(
    @param:Json(name = "commands") val commands: List<BotCommandDto>
)

interface TelegramBotApiService {

    @GET("bot{token}/getMe")
    suspend fun getMe(
        @Path(value = "token", encoded = true) token: String
    ): TelegramResponse<TelegramBotUser>

    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path(value = "token", encoded = true) token: String,
        @Body body: SendMessageRequest
    ): TelegramResponse<TelegramMessageDto>

    @POST("bot{token}/sendDice")
    suspend fun sendDice(
        @Path(value = "token", encoded = true) token: String,
        @Body body: SendDiceRequest
    ): TelegramResponse<TelegramMessageDto>

    @POST("bot{token}/setMyCommands")
    suspend fun setMyCommands(
        @Path(value = "token", encoded = true) token: String,
        @Body body: SetMyCommandsRequest
    ): TelegramResponse<Boolean>

    @GET("bot{token}/getUpdates")
    suspend fun getUpdates(
        @Path(value = "token", encoded = true) token: String,
        @Query("offset") offset: Long? = null,
        @Query("limit") limit: Int = 20,
        @Query("timeout") timeout: Int = 0
    ): TelegramResponse<List<TelegramUpdateDto>>

    companion object {
        const val BASE_URL = "https://api.telegram.org/"

        /**
         * Reads TELEGRAM_BOT_TOKEN injected from .env / .env.example via BuildConfig.
         */
        fun resolveConfiguredToken(rawToken: String = BuildConfig.TELEGRAM_BOT_TOKEN): String? {
            val cleaned = rawToken.trim()
            return if (
                cleaned.isNotEmpty() &&
                cleaned != "YOUR_TELEGRAM_BOT_TOKEN_HERE" &&
                cleaned.contains(":")
            ) {
                cleaned
            } else {
                null
            }
        }

        /**
         * Decodes Telegram's official 1..64 slot machine dice value into a 3-reel symbol row.
         * Per Telegram Bot API docs for emoji="🎰", value-1 is a 6-bit integer where each 2-bit
         * pair encodes Left, Center, and Right reels:
         * 0 -> BAR (🎰), 1 -> Grapes/Diamond (💎), 2 -> Lemon (🍋), 3 -> Seven (7️⃣).
         * Value 64 (bits 11_11_11 = 63) is Triple Sevens (7️⃣ 7️⃣ 7️⃣).
         */
        fun decodeTelegramSlotReels(diceValue: Int): List<SlotSymbol> {
            val v = (diceValue.coerceIn(1, 64) - 1)
            val reelMap = listOf(
                SlotSymbol.BAR,
                SlotSymbol.DIAMOND,
                SlotSymbol.LEMON,
                SlotSymbol.SEVEN
            )
            val left = reelMap[v and 0b11]
            val center = reelMap[(v shr 2) and 0b11]
            val right = reelMap[(v shr 4) and 0b11]
            return listOf(left, center, right)
        }

        fun defaultCasinoBotCommands(): List<BotCommandDto> = listOf(
            BotCommandDto("dice", "Roll 🎲 High Roller Dice (up to 3.0x)"),
            BotCommandDto("darts", "Throw 🎯 Bullseye Darts (up to 4.0x)"),
            BotCommandDto("bowl", "Roll 🎳 Neon Bowling Strike (up to 3.5x)"),
            BotCommandDto("hoops", "Shoot 🏀 Street Hoops (up to 3.2x)"),
            BotCommandDto("goal", "Kick ⚽ Penalty Goal (up to 3.0x)"),
            BotCommandDto("slots", "Spin 🎰 Telegram 1-64 Slot Machine"),
            BotCommandDto("bj", "Deal 🃏 VIP Crypto Blackjack Hand"),
            BotCommandDto("roulette", "Spin 🎡 European Roulette Wheel"),
            BotCommandDto("balance", "Check 💎 TON, USDT & BTC Vault"),
            BotCommandDto("faucet", "Claim 🚰 Free Testnet Crypto"),
            BotCommandDto("seed", "Inspect 🔐 Provably Fair SHA-256 Seed"),
            BotCommandDto("help", "Show 🤖 @TeleVegasCasinoBot Command Menu")
        )

        fun defaultCasinoInlineKeyboard(): InlineKeyboardMarkupDto = InlineKeyboardMarkupDto(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButtonDto("🎲 Roll Dice", "/dice"),
                    InlineKeyboardButtonDto("🎯 Throw Darts", "/darts"),
                    InlineKeyboardButtonDto("🎰 Spin Slots", "/slots")
                ),
                listOf(
                    InlineKeyboardButtonDto("🃏 Blackjack", "/bj"),
                    InlineKeyboardButtonDto("🎡 Roulette", "/roulette"),
                    InlineKeyboardButtonDto("💎 Balance", "/balance")
                )
            )
        )

        fun create(): TelegramBotApiService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val okHttp = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttp)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(TelegramBotApiService::class.java)
        }
    }
}

/**
 * High-level client wrapping [TelegramBotApiService] with automatic resolution of
 * `BuildConfig.TELEGRAM_BOT_TOKEN` for sending formatted casino messages and
 * animated Telegram dice outcomes (`🎲`, `🎯`, `🎳`, `🏀`, `⚽`, `🎰`).
 */
class TelegramBotClient(
    private val service: TelegramBotApiService = TelegramBotApiService.create(),
    private val tokenProvider: () -> String? = { TelegramBotApiService.resolveConfiguredToken() }
) {
    val isTokenConfigured: Boolean
        get() = tokenProvider() != null

    suspend fun verifyBotIdentity(): Result<TelegramBotUser> = runCatching {
        val token = tokenProvider()
            ?: throw IllegalStateException("TELEGRAM_BOT_TOKEN is not configured in BuildConfig.")
        val response = service.getMe(token)
        if (response.ok && response.result != null) {
            response.result
        } else {
            throw IllegalStateException(response.description ?: "Telegram getMe failed")
        }
    }

    suspend fun registerCasinoCommands(): Result<Boolean> = runCatching {
        val token = tokenProvider()
            ?: throw IllegalStateException("TELEGRAM_BOT_TOKEN is not configured in BuildConfig.")
        val response = service.setMyCommands(
            token = token,
            body = SetMyCommandsRequest(TelegramBotApiService.defaultCasinoBotCommands())
        )
        if (response.ok) {
            response.result ?: true
        } else {
            throw IllegalStateException(response.description ?: "Telegram setMyCommands failed")
        }
    }

    suspend fun sendCasinoMessage(
        chatId: String,
        htmlText: String,
        includeInlineKeyboard: Boolean = false
    ): Result<TelegramMessageDto> = runCatching {
        val token = tokenProvider()
            ?: throw IllegalStateException("TELEGRAM_BOT_TOKEN is not configured in BuildConfig.")
        val cleanChatId = chatId.trim()
        require(cleanChatId.isNotEmpty()) { "Target Telegram chat_id is required." }

        val response = service.sendMessage(
            token = token,
            body = SendMessageRequest(
                chatId = cleanChatId,
                text = htmlText,
                parseMode = "HTML",
                replyMarkup = if (includeInlineKeyboard) {
                    TelegramBotApiService.defaultCasinoInlineKeyboard()
                } else {
                    null
                }
            )
        )
        if (response.ok && response.result != null) {
            response.result
        } else {
            throw IllegalStateException(response.description ?: "Telegram sendMessage failed")
        }
    }

    suspend fun sendEmojiDiceRoll(
        chatId: String,
        emoji: String
    ): Result<TelegramDice> = runCatching {
        val token = tokenProvider()
            ?: throw IllegalStateException("TELEGRAM_BOT_TOKEN is not configured in BuildConfig.")
        val cleanChatId = chatId.trim()
        require(cleanChatId.isNotEmpty()) { "Target Telegram chat_id is required." }

        val response = service.sendDice(
            token = token,
            body = SendDiceRequest(chatId = cleanChatId, emoji = emoji)
        )
        val dice = response.result?.dice
        if (response.ok && dice != null) {
            dice
        } else {
            throw IllegalStateException(response.description ?: "Telegram sendDice failed")
        }
    }

    suspend fun fetchUpdates(offset: Long? = null): Result<List<TelegramUpdateDto>> = runCatching {
        val token = tokenProvider()
            ?: throw IllegalStateException("TELEGRAM_BOT_TOKEN is not configured in BuildConfig.")
        val response = service.getUpdates(token = token, offset = offset)
        if (response.ok) {
            response.result.orEmpty()
        } else {
            throw IllegalStateException(response.description ?: "Telegram getUpdates failed")
        }
    }
}
