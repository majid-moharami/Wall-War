package com.wallwar.ui.screens

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
import com.wallwar.ui.components.OfflinePlayDialog
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
    var showOfflinePlayDialog by remember { mutableStateOf(false) }

    if (showSpinnerDialog) {
        LuckySpinnerDialog(
            userCoins = userProfile.coins,
            spinnerState = spinnerState,
            onSpin = onSpinWheel,
            onDismiss = { showSpinnerDialog = false }
        )
    }

    if (showOfflinePlayDialog) {
        OfflinePlayDialog(
            userCoins = userProfile.coins,
            offlineArena = offlineArena,
            isAdPlaying = isAdPlaying,
            isRewardedAdLoading = isRewardedAdLoading,
            onStartOfflineMatch = onJoinOfflineMatch,
            onOpenShop = { onNavigate(AppScreen.COIN_SHOP) },
            onDismiss = { showOfflinePlayDialog = false }
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
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 1. Top Header: Player Avatar, Clean Name (No level badge overlay on avatar), and Coins Pill
        HomeHeaderSection(
            userProfile = userProfile,
            onOpenShop = { onNavigate(AppScreen.COIN_SHOP) },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Global Ranking Tier Card with integrated Win Rate, Total Wins, and Win Streak tags
        HeroRatingCard(
            userProfile = userProfile,
            actualWins = actualWins,
            actualMatches = actualMatches,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Quick Access Retention Hub: Fast one-tap access to Daily Reward & Lucky Spinner
        QuickDailyAccessSection(
            dailyStreakState = dailyStreakState,
            spinnerState = spinnerState,
            onOpenDailyRewardsScreen = { onNavigate(AppScreen.DAILY_REWARDS) },
            onOpenSpinner = { showSpinnerDialog = true },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Online Arena Tables Showcase (Directly on screen with horizontal scroll & animated table previews)
        OnlineArenasSection(
            arenas = onlineArenas,
            userCoins = userProfile.coins,
            onJoinOnlineArenaMatch = onJoinOnlineArenaMatch,
            onOpenShop = { onNavigate(AppScreen.COIN_SHOP) }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 5. Offline & AI Practice Simple Card (Opens the full selection dialog)
        SimpleOfflinePlaySection(
            offlineArena = offlineArena,
            onOpenOfflineDialog = { showOfflinePlayDialog = true },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 7. Daily Quests (If available)
        if (dailyMissions.isNotEmpty()) {
            DailyQuestsSection(
                dailyMissions = dailyMissions,
                onClaimMissionReward = onClaimMissionReward,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // 8. Bottom Navigation Cards (How to Play & Match History, followed by Settings)
        BottomUtilitySection(
            onNavigate = onNavigate,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun HomeHeaderSection(
    userProfile: UserProfile,
    onOpenShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player Profile info (Clean avatar without overlay badge, username and subtitle)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
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
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = userProfile.displayName.ifBlank { "Cyber Gladiator" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        // Coins counter pill (Clickable to open Coin Shop)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(NeonDarkSurface)
                .border(1.dp, NeonAmber.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                .clickable { onOpenShop() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("home_coins_pill")
        ) {
            Text(text = "🪙", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${userProfile.coins} Coins",
                fontWeight = FontWeight.Black,
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
}

@Composable
fun HeroRatingCard(
    userProfile: UserProfile,
    actualWins: Int,
    actualMatches: Int,
    modifier: Modifier = Modifier
) {
    val winRate = if (actualMatches > 0) (actualWins * 100 / actualMatches) else 0

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(NeonCyan, NeonPurple, NeonAmber)
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_rating_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GLOBAL RANKING TIER",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA0ACCC),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = userProfile.rankTitle.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NeonDarkSurface,
                    border = BorderStroke(1.dp, NeonBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ LVL ${userProfile.level}",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = NeonAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trophy / Rating Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Trophies",
                        tint = NeonAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${userProfile.trophies} Trophies",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "${userProfile.xp % 1000} / 1,000 XP",
                    fontSize = 12.sp,
                    color = Color(0xFFA0ACCC),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val progress = ((userProfile.xp % 1000) / 1000f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF202637))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceAtLeast(0.05f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(NeonCyan, NeonPurple, NeonAmber)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Return back Win Rate, Total Wins, and Win Streak tags directly inside the Global Ranking Tier View
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Win Rate Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonDarkSurface,
                    border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🎯 $winRate% Win Rate",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonEmerald
                        )
                    }
                }

                // Total Wins Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonDarkSurface,
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🏆 $actualWins Wins",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }

                // Win Streak Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonDarkSurface,
                    border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🔥 ${userProfile.currentWinStreak} Streak",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickDailyAccessSection(
    dailyStreakState: DailyStreakState,
    spinnerState: SpinnerState,
    onOpenDailyRewardsScreen: () -> Unit,
    onOpenSpinner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Daily Reward Quick Card
        Card(
            onClick = onOpenDailyRewardsScreen,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
            border = BorderStroke(
                1.5.dp,
                if (dailyStreakState.canClaim) SolidColor(NeonAmber)
                else Brush.horizontalGradient(listOf(NeonAmber.copy(alpha = 0.5f), NeonMagenta.copy(alpha = 0.5f)))
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("quick_daily_reward_btn")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(NeonAmber, Color(0xFFB45309)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎁", fontSize = 20.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DAILY REWARD",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                    Text(
                        text = if (dailyStreakState.canClaim) "Day ${dailyStreakState.currentDay} • Ready!" else "Day ${dailyStreakState.currentDay}/7 Streak",
                        fontSize = 10.sp,
                        fontWeight = if (dailyStreakState.canClaim) FontWeight.Bold else FontWeight.Normal,
                        color = if (dailyStreakState.canClaim) NeonAmber else Color(0xFFA0ACCC)
                    )
                }
            }
        }

        // Lucky Spinner Quick Card
        Card(
            onClick = onOpenSpinner,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
            border = BorderStroke(
                1.5.dp,
                if (spinnerState.canSpinToday) SolidColor(NeonCyan)
                else Brush.horizontalGradient(listOf(Color(0xFF2A334A), Color(0xFF1E2435)))
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("quick_spinner_btn")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(NeonMagenta, NeonCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎡", fontSize = 20.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LUCKY WHEEL",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                    Text(
                        text = if (spinnerState.canSpinToday) "🪙 500 • 1/Day" else "Spun Today ✓",
                        fontSize = 10.sp,
                        fontWeight = if (spinnerState.canSpinToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (spinnerState.canSpinToday) NeonCyan else Color(0xFFA0ACCC)
                    )
                }
            }
        }
    }
}


@Composable
fun OnlineArenasSection(
    arenas: List<Arena>,
    userCoins: Int,
    onJoinOnlineArenaMatch: (Arena) -> Unit,
    onOpenShop: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚔️ ONLINE ARENA TABLES",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Text(
                text = "${arenas.size} Tables Available",
                fontSize = 11.sp,
                color = Color(0xFFA0ACCC),
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Carousel of Online Arena Cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
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
fun SimpleOfflinePlaySection(
    offlineArena: Arena,
    onOpenOfflineDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sectionColor = Color(offlineArena.colorHex)

    Card(
        onClick = onOpenOfflineDialog,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(sectionColor.copy(alpha = 0.6f), NeonPurple.copy(alpha = 0.4f), Color(0xFF1E2435))
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("simple_offline_play_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(sectionColor.copy(alpha = 0.35f), Color(0xFF131A2B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = sectionColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⚔️ OFFLINE & PRACTICE",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Play without internet • AI Bot & Pass/Play",
                        fontSize = 11.sp,
                        color = Color(0xFFA0ACCC)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onOpenOfflineDialog,
                colors = ButtonDefaults.buttonColors(containerColor = sectionColor),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("play_offline_btn")
            ) {
                Text(
                    text = "Play",
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun DailyQuestsSection(
    dailyMissions: List<DailyMission>,
    onClaimMissionReward: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
        border = BorderStroke(1.dp, NeonBorder),
        modifier = modifier.fillMaxWidth()
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

@Composable
fun BottomUtilitySection(
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row with 2 cards: How to Play (Rules) and Match History (Stats)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Rules & Guide Card
            Card(
                onClick = { onNavigate(AppScreen.RULES) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                border = BorderStroke(1.dp, NeonBorder),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_rules")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Rules",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "How to Play",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Rules & Guide",
                            fontSize = 10.sp,
                            color = Color(0xFFA0ACCC)
                        )
                    }
                }
            }

            // Match History & Stats Card
            Card(
                onClick = { onNavigate(AppScreen.HISTORY) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                border = BorderStroke(1.dp, NeonBorder),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_stats_history")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "History",
                            tint = NeonAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Match History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Logs & Stats",
                            fontSize = 10.sp,
                            color = Color(0xFFA0ACCC)
                        )
                    }
                }
            }
        }

        // Settings & Preferences Card
        Card(
            onClick = { onNavigate(AppScreen.SETTINGS) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
            border = BorderStroke(1.dp, NeonBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_settings")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = NeonPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Settings & Audio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Sound effects, board themes & cloud account sync",
                            fontSize = 10.sp,
                            color = Color(0xFFA0ACCC)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFFA0ACCC),
                    modifier = Modifier.size(16.dp)
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
            .height(100.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = arenaColor.copy(alpha = 0.4f),
                ambientColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(NeonDarkSurface)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        arenaColor.copy(alpha = alphaGlow),
                        arenaColor.copy(alpha = 0.25f),
                        NeonCyan.copy(alpha = alphaGlow)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cols = 7
            val rows = 5

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
            val redY = 4f + 2 * (cellH + 4f) + cellH / 2f

            val blueX = 4f + 5 * (cellW + 4f) + cellW / 2f
            val blueY = 4f + 2 * (cellH + 4f) + cellH / 2f

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
            val wallY = 4f + 1 * (cellH + 4f)
            val wallH = (cellH * 3f) + 8f

            drawRoundRect(
                color = arenaColor.copy(alpha = alphaGlow),
                topLeft = Offset(wallX, wallY),
                size = Size(5f, wallH),
                cornerRadius = CornerRadius(2.5f, 2.5f)
            )

            // Decorative Corner Bracket Accents
            val cornerL = 10f
            val strokeW = 2.5f

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
