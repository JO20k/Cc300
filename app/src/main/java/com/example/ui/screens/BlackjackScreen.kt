package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlackjackStage
import com.example.data.model.BlackjackState
import com.example.data.model.CryptoCurrency
import com.example.data.model.PlayerProfile
import com.example.data.model.PlayingCard
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TeleCyan
import com.example.ui.theme.VegasGold
import com.example.ui.viewmodel.AiAssistantState
import com.example.ui.viewmodel.CasinoViewModel

@Composable
fun BlackjackScreen(
    profile: PlayerProfile,
    state: BlackjackState,
    aiAssistantState: AiAssistantState = AiAssistantState.Idle,
    onSetBet: (Double) -> Unit,
    onDeal: () -> Unit,
    onHit: () -> Unit,
    onStand: () -> Unit,
    onDoubleDown: () -> Unit,
    onAnalyzeWithAi: () -> Unit = {},
    onDismissAi: () -> Unit = {}
) {
    val currency = CryptoCurrency.fromCode(profile.activeCurrency)
    val isPlaying = state.stage == BlackjackStage.PLAYER_TURN

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Table Felt Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(listOf(NeonEmerald, VegasGold, TeleCyan)),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF08261A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0F3D29),
                                Color(0xFF061B12)
                            )
                        )
                    )
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🃏 VIP CRYPTO BLACKJACK",
                        style = MaterialTheme.typography.titleMedium,
                        color = VegasGold
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0x55000000)
                    ) {
                        Text(
                            text = "BLACKJACK PAYS 3:2 • DEALER STANDS 17",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonEmerald,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Dealer Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (state.dealerHand.isEmpty()) {
                            "DEALER HAND"
                        } else {
                            "DEALER HAND • ${state.dealerVisibleScore}${if (state.dealerHidden) "+" else ""}"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.dealerHand.isEmpty()) {
                            EmptyCardSlot()
                            EmptyCardSlot()
                        } else {
                            state.dealerHand.forEachIndexed { index, card ->
                                if (index == 1 && state.dealerHidden) {
                                    HiddenPlayingCardView()
                                } else {
                                    PlayingCardView(card = card)
                                }
                            }
                        }
                    }
                }

                // Status Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x66000000)
                ) {
                    Text(
                        text = state.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (state.outcome) {
                            "JACKPOT", "WIN" -> NeonEmerald
                            "LOSS" -> CrimsonAlert
                            else -> Color.White
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Player Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.playerHand.isEmpty()) {
                            EmptyCardSlot()
                            EmptyCardSlot()
                        } else {
                            state.playerHand.forEach { card ->
                                PlayingCardView(card = card)
                            }
                        }
                    }
                    Text(
                        text = if (state.playerHand.isEmpty()) {
                            "YOUR HAND"
                        } else {
                            "YOUR HAND • ${state.playerScore}"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = VegasGold
                    )
                }
            }
        }

        // Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wager Amount (${currency.symbol} ${currency.code})",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Text(
                        text = "${CasinoViewModel.formatCrypto(state.betAmount, currency)} ${currency.code}",
                        style = MaterialTheme.typography.titleMedium,
                        color = VegasGold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currency.defaultQuickBets.forEach { amt ->
                        FilterChip(
                            selected = state.betAmount == amt,
                            onClick = { onSetBet(amt) },
                            enabled = !isPlaying,
                            label = {
                                Text(
                                    text = "${CasinoViewModel.formatCrypto(amt, currency)} ${currency.code}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VegasGold.copy(alpha = 0.25f),
                                selectedLabelColor = VegasGold
                            )
                        )
                    }
                }

                if (!isPlaying) {
                    Button(
                        onClick = onDeal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("blackjack_deal_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonEmerald,
                            contentColor = Color(0xFF00210E)
                        )
                    ) {
                        Text(
                            text = "🃏 DEAL BLACKJACK HAND (${CasinoViewModel.formatCrypto(state.betAmount, currency)} ${currency.code})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onHit,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("blackjack_hit_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TeleCyan,
                                contentColor = Color(0xFF031926)
                            )
                        ) {
                            Text(
                                text = "HIT",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onStand,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("blackjack_stand_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VegasGold,
                                contentColor = Color(0xFF261900)
                            )
                        ) {
                            Text(
                                text = "STAND",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (state.playerHand.size == 2) {
                            OutlinedButton(
                                onClick = onDoubleDown,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("blackjack_double_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = "DOUBLE 2x",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NeonEmerald
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onAnalyzeWithAi,
                    enabled = aiAssistantState !is AiAssistantState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("blackjack_ai_advisor_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (aiAssistantState is AiAssistantState.Loading || aiAssistantState is AiAssistantState.Streaming) {
                            "✨ Gemini 3.8 Flash Analyzing Hand..."
                        } else {
                            "✨ Ask Gemini AI Pit Boss (Optimal Move)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = VegasGold
                    )
                }

                if (aiAssistantState !is AiAssistantState.Idle) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VegasGold.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
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
                                    text = "✨ Gemini 3.8 Flash Strategy Advisor",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VegasGold,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(
                                    onClick = onDismissAi,
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Dismiss",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TeleCyan
                                    )
                                }
                            }
                            val text = when (aiAssistantState) {
                                is AiAssistantState.Loading -> "Calculating Blackjack EV & optimal play..."
                                is AiAssistantState.Streaming -> aiAssistantState.text
                                is AiAssistantState.Success -> aiAssistantState.text
                                is AiAssistantState.Error -> "⚠️ ${aiAssistantState.message}"
                                AiAssistantState.Idle -> ""
                            }
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (aiAssistantState is AiAssistantState.Error) CrimsonAlert else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayingCardView(card: PlayingCard) {
    val textColor = if (card.isRed) Color(0xFFE53935) else Color(0xFF111622)
    Surface(
        modifier = Modifier
            .width(68.dp)
            .height(96.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFDFEFE),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = card.rank,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = card.suit,
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = card.rank,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun HiddenPlayingCardView() {
    Surface(
        modifier = Modifier
            .width(68.dp)
            .height(96.dp)
            .border(1.5.dp, VegasGold, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF152238)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = "🂠", fontSize = 32.sp)
        }
    }
}

@Composable
private fun EmptyCardSlot() {
    Box(
        modifier = Modifier
            .width(68.dp)
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, ObsidianElevated, RoundedCornerShape(12.dp))
            .background(Color(0x22000000)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "♠️",
            fontSize = 20.sp,
            color = Color.White.copy(alpha = 0.2f)
        )
    }
}
