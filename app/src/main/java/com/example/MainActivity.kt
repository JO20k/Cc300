package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.CryptoCurrency
import com.example.data.model.PlayerProfile
import com.example.data.repository.CasinoRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.BlackjackScreen
import com.example.ui.screens.BotChatScreen
import com.example.ui.screens.CryptoWalletScreen
import com.example.ui.screens.RouletteScreen
import com.example.ui.screens.SlotsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TeleCyan
import com.example.ui.theme.VegasGold
import com.example.ui.viewmodel.CasinoViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val displayName = user.displayName
            ?: user.email?.substringBefore("@")
            ?: "TelePlayer"
        TeleVegasMainScreen(
            currentUserId = user.uid,
            defaultDisplayName = displayName
        )
    }
}

private data class NavTabItem(
    val index: Int,
    val labelRes: Int,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun TeleVegasMainScreen(
    currentUserId: String,
    defaultDisplayName: String
) {
    val viewModel: CasinoViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CasinoViewModel(
                    repository = CasinoRepository(db),
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val selectedEmojiGame by viewModel.selectedEmojiGame.collectAsStateWithLifecycle()
    val emojiBetAmount by viewModel.emojiBetAmount.collectAsStateWithLifecycle()
    val isRollingEmoji by viewModel.isRollingEmoji.collectAsStateWithLifecycle()
    val lastEmojiAnimationValue by viewModel.lastEmojiAnimationValue.collectAsStateWithLifecycle()
    val blackjackState by viewModel.blackjackState.collectAsStateWithLifecycle()
    val slotsState by viewModel.slotsState.collectAsStateWithLifecycle()
    val rouletteState by viewModel.rouletteState.collectAsStateWithLifecycle()
    val liveBotState by viewModel.liveBotState.collectAsStateWithLifecycle()
    val bannerNotification by viewModel.bannerNotification.collectAsStateWithLifecycle()
    val aiAssistantState by viewModel.aiAssistantState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (selectedTab != 0) {
        BackHandler {
            selectedTab = 0
        }
    }

    val tabs = listOf(
        NavTabItem(0, R.string.tab_bot_chat, Icons.AutoMirrored.Filled.Chat, "nav_tab_bot"),
        NavTabItem(1, R.string.tab_blackjack, Icons.Default.Style, "nav_tab_blackjack"),
        NavTabItem(2, R.string.tab_slots, Icons.Default.Casino, "nav_tab_slots"),
        NavTabItem(3, R.string.tab_roulette, Icons.Default.Attractions, "nav_tab_roulette"),
        NavTabItem(4, R.string.tab_wallet, Icons.Default.AccountBalanceWallet, "nav_tab_wallet")
    )

    val activeProfile = (profileState as? UiState.Success)?.data ?: PlayerProfile(
        id = currentUserId,
        userId = currentUserId,
        username = defaultDisplayName
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBackground,
        topBar = {
            TopCryptoBalanceHeader(
                profile = activeProfile,
                onSelectCurrency = viewModel::selectCurrency,
                onOpenWallet = { selectedTab = 4 },
                onSignOut = {
                    signOutUser(
                        context = context,
                        onSignOutComplete = {},
                        scope = scope
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ObsidianSurface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                tabs.forEach { tab ->
                    val selected = selectedTab == tab.index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab.index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = stringResource(tab.labelRes)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF041521),
                            selectedTextColor = TeleCyan,
                            indicatorColor = TeleCyan,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(visible = bannerNotification != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clickable { viewModel.dismissBanner() },
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianElevated
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = bannerNotification.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = VegasGold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Dismiss",
                            style = MaterialTheme.typography.labelSmall,
                            color = TeleCyan
                        )
                    }
                }
            }

            if (profileState is UiState.Loading && messagesState is UiState.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TeleCyan)
                }
            } else {
                val messages = (messagesState as? UiState.Success)?.data.orEmpty()
                val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()

                when (selectedTab) {
                    0 -> BotChatScreen(
                        profile = activeProfile,
                        messages = messages,
                        selectedEmojiGame = selectedEmojiGame,
                        emojiBetAmount = emojiBetAmount,
                        isRollingEmoji = isRollingEmoji,
                        lastEmojiAnimationValue = lastEmojiAnimationValue,
                        liveBotState = liveBotState,
                        aiAssistantState = aiAssistantState,
                        onSelectEmojiGame = viewModel::selectEmojiGame,
                        onSetEmojiBet = viewModel::setEmojiBetAmount,
                        onPlayEmojiGame = { viewModel.playEmojiGame() },
                        onSendCommand = { viewModel.handleCommandInput(it) },
                        onPollTelegram = viewModel::pollLiveTelegramUpdates,
                        onUpdateChatId = viewModel::updateBotChatId,
                        onTestBroadcast = viewModel::sendTestTelegramBroadcast,
                        onClearChat = { viewModel.handleCommandInput("/clear") },
                        onDismissAi = viewModel::dismissAiAdvice
                    )
                    1 -> BlackjackScreen(
                        profile = activeProfile,
                        state = blackjackState,
                        aiAssistantState = aiAssistantState,
                        onSetBet = viewModel::setBlackjackBet,
                        onDeal = viewModel::dealBlackjack,
                        onHit = viewModel::hitBlackjack,
                        onStand = viewModel::standBlackjack,
                        onDoubleDown = viewModel::doubleDownBlackjack,
                        onAnalyzeWithAi = viewModel::analyzeBlackjackHandWithAi,
                        onDismissAi = viewModel::dismissAiAdvice
                    )
                    2 -> SlotsScreen(
                        profile = activeProfile,
                        state = slotsState,
                        onSetBet = viewModel::setSlotsBet,
                        onSpin = viewModel::spinSlots
                    )
                    3 -> RouletteScreen(
                        profile = activeProfile,
                        state = rouletteState,
                        onSelectBetType = viewModel::selectRouletteBetType,
                        onSelectExactNumber = viewModel::selectRouletteExactNumber,
                        onSetBet = viewModel::setRouletteBet,
                        onSpin = viewModel::spinRoulette
                    )
                    4 -> CryptoWalletScreen(
                        profile = activeProfile,
                        transactions = transactions,
                        onSelectCurrency = viewModel::selectCurrency,
                        onClaimFaucet = viewModel::claimCryptoFaucet,
                        onDeposit = viewModel::simulateCryptoDeposit,
                        onWithdraw = viewModel::withdrawCrypto,
                        onSwap = viewModel::swapCrypto,
                        onRotateSeed = viewModel::updateClientSeed
                    )
                }
            }
        }
    }
}

@Composable
private fun TopCryptoBalanceHeader(
    profile: PlayerProfile,
    onSelectCurrency: (CryptoCurrency) -> Unit,
    onOpenWallet: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        color = ObsidianSurface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Currency Selector Pills
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CryptoCurrency.entries.forEach { currency ->
                    val isSelected = profile.activeCurrency == currency.code
                    val bal = profile.balanceFor(currency)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) NeonEmerald.copy(alpha = 0.18f) else ObsidianSurfaceVariant
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) NeonEmerald else ObsidianElevated,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectCurrency(currency) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("currency_chip_${currency.code.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = currency.symbol, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = CasinoViewModel.formatCrypto(bal, currency),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) NeonEmerald else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currency.code,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenWallet() }
                        .padding(horizontal = 4.dp),
                    color = VegasGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "VIP ${profile.vipLevel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VegasGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = stringResource(R.string.cd_sign_out),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
