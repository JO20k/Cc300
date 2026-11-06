package com.example

import com.example.data.model.SlotSymbol
import com.example.data.remote.TelegramBotApiService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun resolveConfiguredToken_validatesBotFatherFormat() {
        assertNull(TelegramBotApiService.resolveConfiguredToken(""))
        assertNull(TelegramBotApiService.resolveConfiguredToken("YOUR_TELEGRAM_BOT_TOKEN_HERE"))
        assertNull(TelegramBotApiService.resolveConfiguredToken("invalid_token_without_colon"))

        val validToken = "123456789:AAH_testTokenValue12345"
        assertEquals(validToken, TelegramBotApiService.resolveConfiguredToken("  $validToken  "))
    }

    @Test
    fun decodeTelegramSlotReels_decodesOfficialTelegramDiceValues() {
        // Value 1 (bits 00_00_00) -> BAR, BAR, BAR
        assertEquals(
            listOf(SlotSymbol.BAR, SlotSymbol.BAR, SlotSymbol.BAR),
            TelegramBotApiService.decodeTelegramSlotReels(1)
        )
        // Value 64 (bits 11_11_11 = 63) -> SEVEN, SEVEN, SEVEN (Jackpot)
        assertEquals(
            listOf(SlotSymbol.SEVEN, SlotSymbol.SEVEN, SlotSymbol.SEVEN),
            TelegramBotApiService.decodeTelegramSlotReels(64)
        )
    }

    @Test
    fun createTelegramBotApiService_initializesRetrofitClient() {
        val service = TelegramBotApiService.create()
        assertNotNull(service)
        assertEquals(12, TelegramBotApiService.defaultCasinoBotCommands().size)
    }
}
