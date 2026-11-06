package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CryptoCurrency
import com.example.data.model.CryptoTransaction
import com.example.data.model.PlayerProfile
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TeleCyan
import com.example.ui.theme.VegasGold
import com.example.ui.viewmodel.CasinoViewModel
import java.util.Locale

@Composable
fun CryptoWalletScreen(
    profile: PlayerProfile,
    transactions: List<CryptoTransaction>,
    onSelectCurrency: (CryptoCurrency) -> Unit,
    onClaimFaucet: (CryptoCurrency) -> Unit,
    onDeposit: (CryptoCurrency, Double) -> Unit,
    onWithdraw: (CryptoCurrency, Double, String) -> Unit,
    onSwap: (CryptoCurrency, CryptoCurrency, Double) -> Unit,
    onRotateSeed: (String) -> Unit
) {
    val totalUsd = profile.tonBalance * CryptoCurrency.TON.usdRate +
        profile.usdtBalance * CryptoCurrency.USDT.usdRate +
        profile.btcBalance * CryptoCurrency.BTC.usdRate

    var withdrawDialogCurrency by remember { mutableStateOf<CryptoCurrency?>(null) }
    var swapFrom by remember { mutableStateOf(CryptoCurrency.TON) }
    var swapTo by remember { mutableStateOf(CryptoCurrency.USDT) }
    var swapAmountText by remember { mutableStateOf("10") }
    var clientSeedText by remember(profile.clientSeed) { mutableStateOf(profile.clientSeed) }

    if (withdrawDialogCurrency != null) {
        val targetCurr = withdrawDialogCurrency!!
        var amountStr by remember { mutableStateOf(targetCurr.defaultQuickBets.first().toString()) }
        var destAddress by remember { mutableStateOf(targetCurr.sampleDepositAddress) }
        AlertDialog(
            onDismissRequest = { withdrawDialogCurrency = null },
            title = { Text("Withdraw ${targetCurr.displayName} (${targetCurr.code})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Network: ${targetCurr.networkName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TeleCyan
                    )
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount (${targetCurr.code})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = destAddress,
                        onValueChange = { destAddress = it },
                        label = { Text("Destination Wallet Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        onWithdraw(targetCurr, amt, destAddress)
                        withdrawDialogCurrency = null
                    }
                ) {
                    Text("Broadcast Withdrawal")
                }
            },
            dismissButton = {
                TextButton(onClick = { withdrawDialogCurrency = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Portfolio Summary & VIP Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(listOf(TeleCyan, NeonEmerald, VegasGold)),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF10243E), ObsidianSurface)
                        )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MULTI-CHAIN CRYPTO VAULT",
                        style = MaterialTheme.typography.labelLarge,
                        color = TeleCyan
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = VegasGold.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "VIP LEVEL ${profile.vipLevel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VegasGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "$${String.format(Locale.US, "%,.2f", totalUsd)} USD",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatColumn(
                        label = "Total Wagered",
                        value = "$${String.format(Locale.US, "%,.2f", profile.totalWagered)}"
                    )
                    StatColumn(
                        label = "Total Won",
                        value = "$${String.format(Locale.US, "%,.2f", profile.totalWon)}"
                    )
                    StatColumn(
                        label = "Games Played",
                        value = profile.gamesPlayed.toString()
                    )
                }
            }
        }

        // 2. Asset Cards (TON, USDT, BTC)
        CryptoCurrency.entries.forEach { currency ->
            val bal = profile.balanceFor(currency)
            val usdEq = bal * currency.usdRate
            val isActive = profile.activeCurrency == currency.code

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isActive) 1.5.dp else 1.dp,
                        color = if (isActive) NeonEmerald else ObsidianElevated,
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = currency.symbol, fontSize = 26.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${currency.displayName} (${currency.code})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = currency.networkName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${CasinoViewModel.formatCrypto(bal, currency)} ${currency.code}",
                                style = MaterialTheme.typography.titleMedium,
                                color = VegasGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "≈ $${String.format(Locale.US, "%,.2f", usdEq)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Deposit Address: ${currency.sampleDepositAddress}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TeleCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onClaimFaucet(currency) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("faucet_button_${currency.code.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonEmerald,
                                contentColor = Color(0xFF00210E)
                            )
                        ) {
                            Text(
                                text = "+${CasinoViewModel.formatCrypto(currency.faucetAmount, currency)} Faucet",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        OutlinedButton(
                            onClick = { onDeposit(currency, currency.faucetAmount * 2) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Deposit",
                                style = MaterialTheme.typography.labelMedium,
                                color = TeleCyan
                            )
                        }

                        OutlinedButton(
                            onClick = { withdrawDialogCurrency = currency },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Withdraw",
                                style = MaterialTheme.typography.labelMedium,
                                color = VegasGold
                            )
                        }
                    }

                    if (!isActive) {
                        TextButton(
                            onClick = { onSelectCurrency(currency) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Set as Active Betting Currency →", color = NeonEmerald)
                        }
                    }
                }
            }
        }

        // 3. Instant Cross-Chain Swap Card
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
                    text = "⚡ Instant Zero-Fee Crypto Swap",
                    style = MaterialTheme.typography.titleMedium,
                    color = VegasGold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("From:", style = MaterialTheme.typography.labelMedium)
                    CryptoCurrency.entries.forEach { c ->
                        FilterChip(
                            selected = swapFrom == c,
                            onClick = {
                                swapFrom = c
                                if (swapTo == c) {
                                    swapTo = CryptoCurrency.entries.first { it != c }
                                }
                            },
                            label = { Text(c.code) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("To:    ", style = MaterialTheme.typography.labelMedium)
                    CryptoCurrency.entries.filter { it != swapFrom }.forEach { c ->
                        FilterChip(
                            selected = swapTo == c,
                            onClick = { swapTo = c },
                            label = { Text(c.code) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = swapAmountText,
                        onValueChange = { swapAmountText = it },
                        label = { Text("Swap Amount (${swapFrom.code})") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val amt = swapAmountText.toDoubleOrNull() ?: 0.0
                            onSwap(swapFrom, swapTo, amt)
                        },
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("swap_crypto_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Swap → ${swapTo.code}")
                    }
                }
            }
        }

        // 4. Provably Fair SHA-256 Verifier
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
                    text = "🔐 Provably Fair SHA-256 Seed Verifier",
                    style = MaterialTheme.typography.titleMedium,
                    color = TeleCyan
                )
                Text(
                    text = "Server Seed SHA-256: ${profile.serverSeedHash}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Current Bet Nonce: #${profile.nonce}",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonEmerald
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = clientSeedText,
                        onValueChange = { clientSeedText = it },
                        label = { Text("Client Seed") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onRotateSeed(clientSeedText) },
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("rotate_seed_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Rotate")
                    }
                }
            }
        }

        // 5. Recent Blockchain & Casino Transactions Ledger
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
                    text = "📜 Cloud Crypto Ledger (${transactions.size} Records)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                if (transactions.isEmpty()) {
                    Text(
                        text = "No transactions recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    transactions.take(15).forEach { tx ->
                        val isPositive = tx.txType in setOf("DEPOSIT", "FAUCET", "BET_WIN", "SWAP")
                        val curr = CryptoCurrency.fromCode(tx.currency)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianSurfaceVariant)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${tx.txType} • ${tx.addressOrNote}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Hash: ${tx.txHash}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${if (isPositive) "+" else "-"}${CasinoViewModel.formatCrypto(tx.amount, curr)} ${tx.currency}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isPositive) NeonEmerald else CrimsonAlert
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
