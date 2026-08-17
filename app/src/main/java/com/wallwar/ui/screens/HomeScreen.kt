package com.wallwar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.wallwar.ui.components.DailyStreakRewardsCard
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wallwar.data.Arena
import com.wallwar.data.ArenaConfig
import com.wallwar.data.DailyMission
import com.wallwar.data.DailyStreakState
import com.wallwar.data.SpinOutcome
import com.wallwar.data.SpinnerState
import com.wallwar.data.UserProfile
import com.wallwar.model.AiDifficulty
import com.wallwar.model.BoardTheme
import com.wallwar.model.GameMode
import com.wallwar.model.OpponentType
import com.wallwar.ui.AppScreen
import com.wallwar.ui.components.LuckySpinnerDialog
import com.wallwar.ui.theme.NeonAmber
import com.wallwar.ui.theme.NeonBorder
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonDarkBg
import com.wallwar.ui.theme.NeonDarkCard
import com.wallwar.ui.theme.NeonDarkSurface
import com.wallwar.ui.theme.NeonEmerald
import com.wallwar.ui.theme.NeonMagenta
import com.wallwar.ui.theme.NeonPurple

@Composable
fun HomeScreen(
    userProfile: UserProfile = UserProfile(),
    totalWins: Int,
    totalMatches: Int,
    onlineArenas: List<Arena> = ArenaConfig.onlineArenas,
    offlineArena: Arena = ArenaConfig.offlineAiArena,
    boardTheme: BoardTheme = BoardTheme.ELEGANT_DARK,
    arenaErrorMessage: String? = null,
    bonusMessage: String? = null,
    abandonedMatchNotice: String? = null,
    isAdPlaying: Boolean = false,
    isRewardedAdLoading: Boolean = false,
    dailyStreakState: DailyStreakState = DailyStreakState(),
    dailyMissions: List<DailyMission> = emptyList(),
    spinnerState: SpinnerState = SpinnerState(),
    selectedGameMode: GameMode = GameMode.DUEL,
    onSelectGameMode: (GameMode) -> Unit = {},
    onJoinOnlineArenaMatch: (Arena) -> Unit = {},
    onJoinOfflineMatch: (OpponentType, AiDifficulty, Boolean) -> Unit = { _, _, _ -> },
    onClaimDailyBonus: () -> Unit = {},
    onClaimMissionReward: (String) -> Unit = {},
    onSpinWheel: (Boolean) -> SpinOutcome = { error("No spin handler") },
    onClearArenaError: () -> Unit = {},
    onClearBonusMessage: () -> Unit = {},
    onClearAbandonedMatchNotice: () -> Unit = {},
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val actualWins = totalWins.coerceAtLeast(userProfile.wins)
    val actualMatches = totalMatches.coerceAtLeast(userProfile.totalMatches)

    var showSpinnerDialog by remember { mutableStateOf(false) }

    if (showSpinnerDialog) {
        LuckySpinnerDialog(
            userCoins = userProfile.coins,
            spinnerState = spinnerState,
            onSpin = onSpinWheel,
            onDismiss = { showSpinnerDialog = false }
        )
    }

    // Alert dialog for abandoned match notice
    if (abandonedMatchNotice != null) {
        AlertDialog(
            onDismissRequest = onClearAbandonedMatchNotice,
            containerColor = NeonDarkCard,
            title = {
                Text(
                    text = "Match Forfeited ⚠️",
                    color = NeonAmber,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = abandonedMatchNotice,
                    color = Color(0xFFA0ACCC)
                )
            },
            confirmButton = {
                Button(
                    onClick = onClearAbandonedMatchNotice,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Got It", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Alert dialog for insufficient coins error
    if (arenaErrorMessage != null) {
        AlertDialog(
            onDismissRequest = onClearArenaError,
            containerColor = NeonDarkCard,
            title = {
                Text(
                    text = "Insufficient Coins 🪙",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = arenaErrorMessage,
                    color = Color(0xFFA0ACCC)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearArenaError()
                        onNavigate(AppScreen.COIN_SHOP)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber)
                ) {
                    Text("Open Coin Shop", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onClearArenaError,
                    border = BorderStroke(1.dp, NeonBorder)
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    // Alert dialog for claimed daily bonus / ad reward
    if (bonusMessage != null) {
        AlertDialog(
            onDismissRequest = onClearBonusMessage,
            containerColor = NeonDarkCard,
            title = {
                Text(
                    text = "Reward Received! 🎉",
                    color = NeonAmber,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = bonusMessage,
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = onClearBonusMessage,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber)
                ) {
                    Text("Awesome!", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeonDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NeonDarkSurface)
                        .border(2.dp, NeonCyan, CircleShape)
                        .padding(if (userProfile.photoUrl.isNullOrBlank()) 0.dp else 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!userProfile.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = userProfile.photoUrl,
                            contentDescription = userProfile.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = userProfile.displayName,
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "TACTICAL ARENA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = userProfile.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // Energy / Coins Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(NeonDarkSurface)
                    .border(1.dp, NeonAmber.copy(alpha = 0.8f), CircleShape)
                    .clickable { onNavigate(AppScreen.COIN_SHOP) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("home_coins_pill")
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(NeonAmber.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🪙",
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${userProfile.coins} Coins",
                    fontWeight = FontWeight.Bold,
                    color = NeonAmber,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Buy Coins",
                    tint = NeonAmber,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Rating Section
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
            border = BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(listOf(NeonCyan, NeonMagenta))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "RATING & TROPHIES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${userProfile.trophies} 🏆",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonEmerald.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Win Rate: ${if (actualMatches > 0) (actualWins * 100 / actualMatches) else 0}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonEmerald
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Total Wins: $actualWins",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonAmber
                            )
                        }
                        if (userProfile.currentWinStreak > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonMagenta.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🔥 Streak: ${userProfile.currentWinStreak}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonMagenta
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bento Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🎖️ Tier Rank", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA0ACCC))
                    Text(
                        text = userProfile.rankTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⚔️ Level & XP", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA0ACCC))
                    Text(
                        text = "Lvl ${userProfile.level} (${userProfile.xp} XP)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 🎮 GAME MODE SELECTOR
        GameModeSelectorSection(
            selectedMode = selectedGameMode,
            onSelectMode = onSelectGameMode
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 🌐 1. ONLINE MULTI-PLAYER ARENAS (TABLES LIST)
        OnlineArenasSection(
            userCoins = userProfile.coins,
            arenas = onlineArenas,
            onJoinOnlineArenaMatch = onJoinOnlineArenaMatch,
            onOpenShop = { onNavigate(AppScreen.COIN_SHOP) },
            onClaimDailyBonus = onClaimDailyBonus
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ⚔️ 2. OFFLINE PRACTICE & AI BATTLES
        OfflinePracticeSection(
            userCoins = userProfile.coins,
            offlineArena = offlineArena,
            isAdPlaying = isAdPlaying,
            isRewardedAdLoading = isRewardedAdLoading,
            onJoinOfflineMatch = onJoinOfflineMatch,
            onOpenShop = { onNavigate(AppScreen.COIN_SHOP) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 🎁 DAILY RETENTION HUB (Streak + Lucky Spinner + Daily Missions)
        DailyRetentionHubSection(
            dailyStreakState = dailyStreakState,
            spinnerState = spinnerState,
            dailyMissions = dailyMissions,
            onClaimDailyStreak = onClaimDailyBonus,
            onOpenSpinner = { showSpinnerDialog = true },
            onClaimMissionReward = onClaimMissionReward
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Bottom Menu Cards (Rules & Stats & Settings)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                onClick = { onNavigate(AppScreen.RULES) },
                colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                shape = RoundedCornerShape(16.dp),
                border = null,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_rules")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Rules",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "How to Play",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Rules & Guides",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0ACCC)
                    )
                }
            }

            Card(
                onClick = { onNavigate(AppScreen.HISTORY) },
                colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                shape = RoundedCornerShape(16.dp),
                border = null,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_stats_history")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Stats",
                            tint = NeonAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Match History",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$actualWins W / $actualMatches Matches",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0ACCC)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Settings Button
        Card(
            onClick = { onNavigate(AppScreen.SETTINGS) },
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
            shape = RoundedCornerShape(16.dp),
            border = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("btn_settings")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeonPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = NeonPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Settings & Board Themes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Active Theme: ${boardTheme.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(boardTheme.primaryColor)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFFA0ACCC)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun OnlineArenasSection(
    userCoins: Int,
    arenas: List<Arena>,
    onJoinOnlineArenaMatch: (Arena) -> Unit,
    onOpenShop: () -> Unit,
    onClaimDailyBonus: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MULTIPLAYER TABLES",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Select your stake level",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA0ACCC)
                )
            }

            if (userCoins < 50) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF261D0C),
                    border = BorderStroke(1.dp, NeonAmber),
                    modifier = Modifier.clickable { onClaimDailyBonus() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📺 +25 🪙", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                    }
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(arenas, key = { it.id }) { arena ->
                OnlineArenaCard(
                    arena = arena,
                    userCoins = userCoins,
                    onJoinOnlineArenaMatch = onJoinOnlineArenaMatch,
                    onOpenShop = onOpenShop
                )
            }
        }
    }
}

@Composable
fun TableBoardPreviewAnimation(
    arenaColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TableBoardAnim")
    val alphaGlow by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaGlow"
    )

    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulsePhase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = arenaColor.copy(alpha = 0.45f),
                ambientColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(NeonDarkSurface)
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        arenaColor.copy(alpha = alphaGlow),
                        arenaColor.copy(alpha = 0.3f),
                        NeonCyan.copy(alpha = alphaGlow)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cols = 7
            val rows = 3

            val cellW = (w - (cols + 1) * 4f) / cols
            val cellH = (h - (rows + 1) * 4f) / rows

            // Mini board cells grid with wave pulse
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val x = 4f + c * (cellW + 4f)
                    val y = 4f + r * (cellH + 4f)

                    val waveDist = (c + r) / (cols + rows).toFloat()
                    val isLit = ((pulsePhase + waveDist) % 1.0f) < 0.38f

                    drawRoundRect(
                        color = if (isLit) arenaColor.copy(alpha = 0.55f * alphaGlow) else Color(0xFF161B2B),
                        topLeft = Offset(x, y),
                        size = Size(cellW, cellH),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }

            // Red & Blue / Arena Pawn Previews + Radial Glow
            val redX = 4f + 1 * (cellW + 4f) + cellW / 2f
            val redY = 4f + 1 * (cellH + 4f) + cellH / 2f

            val blueX = 4f + 5 * (cellW + 4f) + cellW / 2f
            val blueY = 4f + 1 * (cellH + 4f) + cellH / 2f

            // Red Pawn Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonMagenta.copy(alpha = 0.8f * alphaGlow), Color.Transparent),
                    center = Offset(redX, redY),
                    radius = cellW * 0.9f
                )
            )
            drawCircle(
                color = NeonMagenta,
                radius = minOf(cellW, cellH) * 0.38f,
                center = Offset(redX, redY)
            )
            drawCircle(
                color = Color.White,
                radius = minOf(cellW, cellH) * 0.18f,
                center = Offset(redX, redY)
            )

            // Blue / Arena Pawn Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(arenaColor.copy(alpha = 0.85f * alphaGlow), Color.Transparent),
                    center = Offset(blueX, blueY),
                    radius = cellW * 0.9f
                )
            )
            drawCircle(
                color = arenaColor,
                radius = minOf(cellW, cellH) * 0.38f,
                center = Offset(blueX, blueY)
            )
            drawCircle(
                color = Color.White,
                radius = minOf(cellW, cellH) * 0.18f,
                center = Offset(blueX, blueY)
            )

            // Wall preview with bright glow
            val wallX = 4f + 3 * (cellW + 4f) - 2f
            val wallY = 4f + 0 * (cellH + 4f)
            val wallH = (cellH * 2f) + 4f

            drawRoundRect(
                color = arenaColor.copy(alpha = alphaGlow),
                topLeft = Offset(wallX, wallY),
                size = Size(5f, wallH),
                cornerRadius = CornerRadius(2.5f, 2.5f)
            )

            // Tactical Corner Frame Viewfinder Lines
            val cornerL = 8.dp.toPx()
            val strokeW = 1.8.dp.toPx()

            drawPath(
                path = Path().apply {
                    moveTo(0f, cornerL)
                    lineTo(0f, 0f)
                    lineTo(cornerL, 0f)
                },
                color = arenaColor.copy(alpha = alphaGlow),
                style = Stroke(width = strokeW)
            )

            drawPath(
                path = Path().apply {
                    moveTo(w - cornerL, 0f)
                    lineTo(w, 0f)
                    lineTo(w, cornerL)
                },
                color = arenaColor.copy(alpha = alphaGlow),
                style = Stroke(width = strokeW)
            )

            drawPath(
                path = Path().apply {
                    moveTo(0f, h - cornerL)
                    lineTo(0f, h)
                    lineTo(cornerL, h)
                },
                color = arenaColor.copy(alpha = alphaGlow),
                style = Stroke(width = strokeW)
            )

            drawPath(
                path = Path().apply {
                    moveTo(w - cornerL, h)
                    lineTo(w, h)
                    lineTo(w, h - cornerL)
                },
                color = arenaColor.copy(alpha = alphaGlow),
                style = Stroke(width = strokeW)
            )
        }
    }
}

@Composable
fun OnlineArenaCard(
    arena: Arena,
    userCoins: Int,
    onJoinOnlineArenaMatch: (Arena) -> Unit,
    onOpenShop: () -> Unit
) {
    val hasEnoughCoins = userCoins >= arena.entryFee
    val arenaColor = Color(arena.colorHex)

    val infiniteTransition = rememberInfiniteTransition(label = "OnlineArenaCardAnim")
    val alphaGlow by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaGlow"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
        border = BorderStroke(
            width = if (arena.isPopular || arena.isBestValue) 1.5.dp else 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    arenaColor.copy(alpha = alphaGlow),
                    arenaColor.copy(alpha = 0.25f),
                    NeonCyan.copy(alpha = alphaGlow),
                    arenaColor.copy(alpha = alphaGlow)
                )
            )
        ),
        modifier = Modifier
            .width(280.dp)
            .shadow(
                elevation = if (arena.isPopular || arena.isBestValue) 10.dp else 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = arenaColor.copy(alpha = 0.5f),
                ambientColor = arenaColor.copy(alpha = 0.25f)
            )
            .testTag("online_arena_card_${arena.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Icon, Title & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(arenaColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (arena.id) {
                                "starter" -> Icons.Default.SportsEsports
                                "novice" -> Icons.Default.Shield
                                "amateur" -> Icons.Default.FlashOn
                                "pro" -> Icons.Default.WorkspacePremium
                                "highroller" -> Icons.Default.FlashOn
                                "master" -> Icons.Default.EmojiEvents
                                "grandchampion" -> Icons.Default.EmojiEvents
                                else -> Icons.Default.Shield
                            },
                            contentDescription = null,
                            tint = arenaColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = arena.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                if (arena.isPopular) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeonCyan
                    ) {
                        Text(
                            text = "POPULAR",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (arena.isBestValue) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeonAmber
                    ) {
                        Text(
                            text = "APEX TIER",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = arena.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA0ACCC),
                fontSize = 11.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Animated Tactical Table Board Preview
            TableBoardPreviewAnimation(arenaColor = arenaColor)

            Spacer(modifier = Modifier.height(10.dp))

            // Stakes Container (Entry Fee vs Winning Prize)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NeonDarkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Entry Fee
                    Column {
                        Text(
                            text = "ENTRY FEE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBAC5E1)
                        )
                        Text(
                            text = "🪙 ${arena.entryFee}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonAmber
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(Color(0xFF22293E))
                    )

                    // Winning Prize
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "WINNER REWARD",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBAC5E1)
                        )
                        Text(
                            text = "🏆 ${arena.winningPrize} Coins",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = arenaColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Join Button
            if (hasEnoughCoins) {
                Button(
                    onClick = { onJoinOnlineArenaMatch(arena) },
                    colors = ButtonDefaults.buttonColors(containerColor = arenaColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("join_online_arena_${arena.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Play (🪙 ${arena.entryFee})",
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onOpenShop,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF261D1F)),
                    border = BorderStroke(1.dp, NeonAmber),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "Need 🪙 ${arena.entryFee}",
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OfflinePracticeSection(
    userCoins: Int,
    offlineArena: Arena,
    isAdPlaying: Boolean = false,
    isRewardedAdLoading: Boolean = false,
    onJoinOfflineMatch: (OpponentType, AiDifficulty, Boolean) -> Unit,
    onOpenShop: () -> Unit
) {
    var selectedOpponent by remember { mutableStateOf(OpponentType.AI) }
    var selectedDifficulty by remember { mutableStateOf(AiDifficulty.NORMAL) }
    val sectionColor = Color(offlineArena.colorHex) // Electric Cyan Blue (0xFF0EA5E9)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
        border = null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("offline_practice_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Title & Notice
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(sectionColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = sectionColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "⚔️ OFFLINE & AI PRACTICE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "No Exploit Farming: 0 Coins Reward",
                            fontSize = 11.sp,
                            color = sectionColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Opponent Selector Tabs (VS AI vs Local Pass & Play)
            Text(
                text = "Select Practice Mode:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA0ACCC)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonDarkSurface)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedOpponent == OpponentType.AI) sectionColor.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable { selectedOpponent = OpponentType.AI }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🤖 VS AI Bot",
                        fontSize = 12.sp,
                        fontWeight = if (selectedOpponent == OpponentType.AI) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedOpponent == OpponentType.AI) Color.White else Color(0xFF6B7A99)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedOpponent == OpponentType.LOCAL_PASS_PLAY) sectionColor.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable { selectedOpponent = OpponentType.LOCAL_PASS_PLAY }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡ Local Pass & Play",
                        fontSize = 12.sp,
                        fontWeight = if (selectedOpponent == OpponentType.LOCAL_PASS_PLAY) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedOpponent == OpponentType.LOCAL_PASS_PLAY) Color.White else Color(0xFF6B7A99)
                    )
                }
            }

            // AI Difficulty Selector
            if (selectedOpponent == OpponentType.AI) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AiDifficulty.entries.forEach { diff ->
                        val isSelected = selectedDifficulty == diff
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) sectionColor.copy(alpha = 0.2f) else Color(0xFF131726))
                                .clickable { selectedDifficulty = diff }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = diff.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) sectionColor else Color(0xFFA0ACCC)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two Entry Options for Offline / AI:
            // Option 1: Pay 50 Coins Entry Fee
            // Option 2: Watch Rewarded Ad (Free Entry)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1: Pay 5 Coins
                val canPay5 = userCoins >= offlineArena.entryFee
                Button(
                    onClick = { onJoinOfflineMatch(selectedOpponent, selectedDifficulty, false) },
                    enabled = canPay5,
                    colors = ButtonDefaults.buttonColors(containerColor = sectionColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_offline_pay_coins")
                ) {
                    Text(
                        text = "Pay 🪙 ${offlineArena.entryFee}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 12.sp
                    )
                }

                // Button 2: Watch Rewarded Ad (Free Entry)
                val isAdBusy = isAdPlaying || isRewardedAdLoading
                Button(
                    onClick = { onJoinOfflineMatch(selectedOpponent, selectedDifficulty, true) },
                    enabled = !isAdBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonAmber,
                        disabledContainerColor = Color(0xFF333A4D),
                        disabledContentColor = Color(0xFF8893A8)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(44.dp)
                        .testTag("btn_offline_ad_free")
                ) {
                    if (isRewardedAdLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Loading Ad...",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    } else if (isAdPlaying) {
                        Text(
                            text = "⏳ Showing Ad...",
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "📺 Watch Ad (Free Entry)",
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameModeSelectorSection(
    selectedMode: GameMode,
    onSelectMode: (GameMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ GAME MODE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = NeonCyan,
                letterSpacing = 1.2.sp
            )
            Text(
                text = selectedMode.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modes = listOf(
                Triple(GameMode.DUEL, "Classic Duel", "10 Walls"),
                Triple(GameMode.QUICK_5V5, "Quick 5v5", "5 Walls"),
                Triple(GameMode.SUDDEN_DEATH, "Sudden Death", "3 Walls")
            )

            modes.forEach { (mode, title, sub) ->
                val isSelected = selectedMode == mode
                Card(
                    onClick = { onSelectMode(mode) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) NeonDarkSurface else NeonDarkCard
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) NeonCyan else NeonBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_${mode.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isSelected) NeonCyan else Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = sub,
                            fontSize = 10.sp,
                            color = if (isSelected) Color(0xFFA0ACCC) else Color(0xFF6B7280),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyRetentionHubSection(
    dailyStreakState: DailyStreakState,
    spinnerState: SpinnerState,
    dailyMissions: List<DailyMission>,
    onClaimDailyStreak: () -> Unit,
    onOpenSpinner: () -> Unit,
    onClaimMissionReward: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Redesigned 7-Day Streak Rewards Card
        DailyStreakRewardsCard(
            dailyStreakState = dailyStreakState,
            onClaim = onClaimDailyStreak
        )

        // 2. Lucky Spinner Banner Card
        Card(
            onClick = onOpenSpinner,
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
            border = BorderStroke(
                1.5.dp,
                if (spinnerState.hasFreeSpin) SolidColor(NeonMagenta)
                else Brush.horizontalGradient(listOf(NeonAmber, NeonCyan))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_open_spinner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(NeonAmber, NeonMagenta))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎡", fontSize = 22.sp)
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "LUCKY WHEEL",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            if (spinnerState.hasFreeSpin) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = NeonEmerald
                                ) {
                                    Text(
                                        text = "FREE SPIN!",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Spin now to win up to 1,000 Coins & XP",
                            fontSize = 11.sp,
                            color = Color(0xFFA0ACCC)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (spinnerState.hasFreeSpin) NeonEmerald else Color(0xFF262C3A),
                    border = if (!spinnerState.hasFreeSpin) BorderStroke(1.dp, NeonBorder) else null
                ) {
                    Text(
                        text = if (spinnerState.hasFreeSpin) "SPIN NOW" else "500 🪙",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = if (spinnerState.hasFreeSpin) Color.Black else NeonAmber,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // 3. Daily Quests Section
        if (dailyMissions.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                border = BorderStroke(1.dp, NeonBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "🎯", fontSize = 16.sp)
                            Text(
                                text = "DAILY QUESTS",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonDarkSurface
                        ) {
                            Text(
                                text = "${dailyMissions.count { it.isClaimed }}/${dailyMissions.size} Done",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    dailyMissions.forEachIndexed { index, mission ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        DailyMissionItemRow(
                            mission = mission,
                            onClaim = { onClaimMissionReward(mission.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyMissionItemRow(
    mission: DailyMission,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NeonDarkSurface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = mission.icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mission.title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
            )
            Text(
                text = mission.description,
                fontSize = 10.sp,
                color = Color(0xFFA0ACCC)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Progress bar
            val progressRatio = (mission.currentProgress.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF2A3142))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressRatio)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (mission.isCompleted) NeonEmerald else NeonCyan)
                    )
                }
                Text(
                    text = "${mission.currentProgress}/${mission.target}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA0ACCC)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (mission.isClaimed) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF262C3A))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text("Claimed", fontSize = 10.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
            }
        } else if (mission.isCompleted) {
            Button(
                onClick = onClaim,
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Claim +${mission.coinReward}🪙", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Text("+${mission.coinReward} 🪙", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                Text("+${mission.xpReward} XP", fontSize = 9.sp, color = NeonCyan)
            }
        }
    }
}
