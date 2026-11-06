package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CryptoCurrency
import com.example.data.model.PlayerProfile
import com.example.data.model.RouletteBetType
import com.example.data.model.RouletteState
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TeleCyan
import com.example.ui.theme.VegasGold
import com.example.ui.viewmodel.CasinoViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RouletteScreen(
    profile: PlayerProfile,
    state: RouletteState,
    onSelectBetType: (RouletteBetType) -> Unit,
    onSelectExactNumber: (Int) -> Unit,
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
        // 1. European Roulette Wheel & Recent Pockets Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(listOf(VegasGold, CrimsonAlert, NeonEmerald)),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎡 EUROPEAN ROULETTE (0–36)",
                        style = MaterialTheme.typography.titleMedium,
                        color = VegasGold
                    )
                    Text(
                        text = "SINGLE ZERO • 97.3% RTP",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonEmerald
                    )
                }

                // Roulette Wheel Visualization + Winning Pocket Hub
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 26.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                        val arcSize = Size(diameter, diameter)
                        val sweep = 360f / 37f

                        for (i in 0..36) {
                            val startAngle = -90f + i * sweep
                            val arcColor = when {
                                i == 0 -> Color(0xFF00C853)
                                RouletteState.RED_NUMBERS.contains(i) -> Color(0xFFD32F2F)
                                else -> Color(0xFF1C2434)
                            }
                            drawArc(
                                color = arcColor,
                                startAngle = startAngle,
                                sweepAngle = sweep - 0.8f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth)
                            )
                        }

                        // Outer Gold Rim
                        drawCircle(
                            color = Color(0xFFFFCA28),
                            radius = size.minDimension / 2f - 2.dp.toPx(),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Center Pocket Display
                    val pocketColor = when {
                        state.lastPocket == 0 -> NeonEmerald
                        RouletteState.RED_NUMBERS.contains(state.lastPocket) -> CrimsonAlert
                        else -> Color(0xFF28334A)
                    }
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(pocketColor)
                            .border(2.dp, VegasGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.lastPocket.toString(),
                                style = MaterialTheme.typography.displayMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = RouletteState.pocketColorName(state.lastPocket),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // Recent Pockets History Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Last Spins:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.recentPockets.forEach { p ->
                        val chipBg = when {
                            p == 0 -> Color(0xFF00C853)
                            RouletteState.RED_NUMBERS.contains(p) -> Color(0xFFC62828)
                            else -> Color(0xFF232D42)
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(chipBg)
                                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

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

        // 2. Betting Board (Outside Bets + Dozens + Straight Number)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Select Board Bet Type",
                    style = MaterialTheme.typography.titleSmall,
                    color = TeleCyan
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RouletteBetType.entries.forEach { betType ->
                        val selected = state.selectedBetType == betType
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectBetType(betType) }
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) VegasGold else ObsidianElevated,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("roulette_bet_${betType.name.lowercase()}"),
                            color = if (selected) VegasGold.copy(alpha = 0.22f) else ObsidianSurfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = betType.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) VegasGold else Color.White
                                )
                                Text(
                                    text = "${betType.multiplier}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonEmerald
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "2. Or Tap Any Straight-Up Number (Pays 36x / 35:1)",
                    style = MaterialTheme.typography.titleSmall,
                    color = TeleCyan
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (0..36).forEach { num ->
                        val isSelectedExact =
                            (state.selectedBetType == RouletteBetType.STRAIGHT_NUMBER && state.selectedNumber == num) ||
                                (state.selectedBetType == RouletteBetType.GREEN_ZERO && num == 0)
                        val numColor = when {
                            num == 0 -> Color(0xFF00C853)
                            RouletteState.RED_NUMBERS.contains(num) -> Color(0xFFC62828)
                            else -> Color(0xFF1F283B)
                        }
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(numColor)
                                .border(
                                    width = if (isSelectedExact) 2.dp else 1.dp,
                                    color = if (isSelectedExact) VegasGold else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectExactNumber(num) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = num.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wager (${currency.symbol} ${currency.code})",
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
                        .height(54.dp)
                        .testTag("spin_roulette_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonEmerald,
                        contentColor = Color(0xFF00210E)
                    )
                ) {
                    val activeLabel = if (state.selectedBetType == RouletteBetType.STRAIGHT_NUMBER) {
                        "Straight #${state.selectedNumber} (36x)"
                    } else {
                        "${state.selectedBetType.label} (${state.selectedBetType.multiplier}x)"
                    }
                    Text(
                        text = if (state.isSpinning) {
                            "🎡 SPINNING WHEEL..."
                        } else {
                            "🎡 SPIN WHEEL • $activeLabel"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
