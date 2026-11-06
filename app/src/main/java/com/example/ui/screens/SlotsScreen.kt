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
import com.example.data.model.CryptoCurrency
import com.example.data.model.PlayerProfile
import com.example.data.model.SlotSymbol
import com.example.data.model.SlotsState
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.TeleCyan
import com.example.ui.theme.VegasGold
import com.example.ui.viewmodel.CasinoViewModel

@Composable
fun SlotsScreen(
    profile: PlayerProfile,
    state: SlotsState,
    onSetBet: (Double) -> Unit,
    onSpin: () -> Unit
) {
    val currency = CryptoCurrency.fromCode(profile.activeCurrency)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Cyber Slot Machine Cabinet
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(listOf(VegasGold, RoyalPurple, TeleCyan)),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140D26))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E1338),
                                Color(0xFF0E091A)
                            )
                        )
                    )
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎰 777 CYBER SLOTS",
                        style = MaterialTheme.typography.titleMedium,
                        color = VegasGold
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0x55000000)
                    ) {
                        Text(
                            text = "TELEGRAM 🎰 ROLL #${state.telegramSlotValue}/64",
                            style = MaterialTheme.typography.labelSmall,
                            color = TeleCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // 3x3 Slot Reel Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF07050E))
                        .border(1.5.dp, VegasGold.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.grid.forEachIndexed { rowIndex, rowSymbols ->
                        val isRowWinner = state.winningLines.contains(rowIndex)
                        val isCenterLine = rowIndex == 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    when {
                                        isRowWinner -> NeonEmerald.copy(alpha = 0.22f)
                                        isCenterLine -> VegasGold.copy(alpha = 0.10f)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isRowWinner || isCenterLine) 1.dp else 0.dp,
                                    color = when {
                                        isRowWinner -> NeonEmerald
                                        isCenterLine -> VegasGold.copy(alpha = 0.5f)
                                        else -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowSymbols.forEach { symbol ->
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(ObsidianSurfaceVariant)
                                        .border(1.dp, ObsidianElevated, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = symbol.emoji, fontSize = 32.sp)
                                        Text(
                                            text = symbol.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Status & Last Payout Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x66000000)
                ) {
                    Text(
                        text = state.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.lastPayout > 0) NeonEmerald else Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // 2. Bet Controls & Spin Action
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
                        text = "Spin Wager (${currency.symbol} ${currency.code})",
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
                            enabled = !state.isSpinning,
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

                Button(
                    onClick = onSpin,
                    enabled = !state.isSpinning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("spin_slots_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VegasGold,
                        contentColor = Color(0xFF231700)
                    )
                ) {
                    Text(
                        text = if (state.isSpinning) {
                            "🎰 SPINNING REELS..."
                        } else {
                            "🎰 SPIN 5-LINE REELS (${CasinoViewModel.formatCrypto(state.betAmount, currency)} ${currency.code})"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Paytable Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "5-Payline Multiplier Paytable (3 Horizontal + 2 Diagonals)",
                    style = MaterialTheme.typography.titleSmall,
                    color = TeleCyan
                )
                SlotSymbol.entries.forEach { sym ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianSurfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${sym.emoji} ${sym.emoji} ${sym.emoji}  ${sym.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            text = "3x: ${sym.tripleMultiplier}x  |  Center 2x: ${sym.doubleMultiplier}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = VegasGold
                        )
                    }
                }
            }
        }
    }
}
