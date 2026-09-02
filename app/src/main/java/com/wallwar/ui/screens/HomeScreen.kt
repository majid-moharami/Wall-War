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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wallwar.R
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
            onOpenShop = {
                showOfflinePlayDialog = false
                onNavigate(AppScreen.COIN_SHOP)
            },
            onDismiss = {
                if (!isRewardedAdLoading && !isAdPlaying) {
                    showOfflinePlayDialog = false
                }
            }
        )
    }

    // Alert dialog for abandoned match notice
    if (abandonedMatchNotice != null) {
        AlertDialog(
            onDismissRequest = onClearAbandonedMatchNotice,
            containerColor = NeonDarkCard,
            title = {
                Text(
                    text = stringResource(R.string.home_forfeit_title),
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
                    Text(stringResource(R.string.btn_got_it), color = Color.Black, fontWeight = FontWeight.Bold)
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
                    text = stringResource(R.string.home_insufficient_coins_title),
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
                    Text(stringResource(R.string.home_open_coin_shop), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onClearArenaError,
                    border = BorderStroke(1.dp, NeonBorder)
                ) {
                    Text(stringResource(R.string.btn_cancel), color = Color.White)
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
                    text = stringResource(R.string.home_reward_received_title),
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
                    Text(stringResource(R.string.btn_awesome), color = Color.Black, fontWeight = FontWeight.Bold)
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

        // 1. Top Header: Player Avatar on left, Coins + Skins + Daily Quests + Settings on right
        HomeHeaderSection(
            userProfile = userProfile,
            dailyMissions = dailyMissions,
            onOpenProfile = { onNavigate(AppScreen.PROFILE) },
            onOpenShop = { onNavigate(AppScreen.COIN_SHOP) },
            onOpenSkins = { onNavigate(AppScreen.SKIN_SHOP) },
            onOpenDailyQuests = { onNavigate(AppScreen.DAILY_QUESTS) },
            onOpenSettings = { onNavigate(AppScreen.SETTINGS) },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Global Ranking Tier Card with integrated Win Rate, Total Wins, and Win Streak tags (Click to open Daily Quests)
        HeroRatingCard(
            userProfile = userProfile,
            actualWins = actualWins,
            actualMatches = actualMatches,
            onClick = { onNavigate(AppScreen.DAILY_QUESTS) },
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

        // 6. Bottom Navigation Cards (How to Play & Match History, followed by Settings)
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
    dailyMissions: List<DailyMission>,
    onOpenProfile: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenSkins: () -> Unit,
    onOpenDailyQuests: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unClaimedQuestsCount = dailyMissions.count { it.isCompleted && !it.isClaimed }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player Profile Avatar only (No text name/description to keep top bar minimalist & spacious)
        Box(
            modifier = Modifier
                .clickable { onOpenProfile() }
                .testTag("home_profile_avatar")
        ) {
            com.wallwar.ui.components.AvatarBadge(
                photoUrl = userProfile.photoUrl,
                size = 46.dp,
                borderWidth = 2.dp
            )
        }

        // Right side: Coins Pill + Skins Button + Settings Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Coins counter pill (Clickable to open Coin Shop)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(NeonDarkSurface)
                    .border(1.dp, NeonAmber.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .clickable { onOpenShop() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("home_coins_pill")
            ) {
                Text(text = "🪙", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${userProfile.coins}",
                    fontWeight = FontWeight.Black,
                    color = NeonAmber,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = stringResource(R.string.home_buy_coins),
                    tint = NeonAmber,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Skins Armory Shining Moving Button with Glowing Shadow, Badge & "Skins" Text Label
            ShiningSkinButton(
                onClick = onOpenSkins,
                modifier = Modifier.testTag("home_skins_btn")
            )

            // Settings Icon Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(NeonDarkSurface)
                    .border(1.dp, NeonBorder, CircleShape)
                    .clickable { onOpenSettings() }
                    .testTag("home_settings_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = Color(0xFFBAC5E1),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ShiningSkinButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SkinBtnDynamicAnimation")

    // 1. Smooth dynamic color phase (0f to 1f)
    val colorPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ColorPhase"
    )

    // 2. Subtle breath scale
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScalePulse"
    )

    // 3. Specular shine sweep across pill
    val shineProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShineProgress"
    )

    // Dynamic color sequence: Gold -> Neon Magenta -> Cyber Purple -> Electric Cyan -> Neon Emerald -> Gold
    val dynamicColors = listOf(
        Color(0xFFFFD54F), // Gold
        NeonMagenta,       // Magenta
        NeonPurple,        // Purple
        NeonCyan,          // Cyan
        NeonEmerald,       // Emerald
        Color(0xFFFFD54F)  // Back to Gold
    )

    // Interpolate current primary & secondary colors for dynamic shadow & highlights
    val totalStops = dynamicColors.size - 1
    val scaledProgress = (colorPhase * totalStops).coerceIn(0f, totalStops.toFloat())
    val currentIndex = scaledProgress.toInt().coerceIn(0, totalStops - 1)
    val localFraction = scaledProgress - currentIndex
    val activeColor1 = dynamicColors[currentIndex]
    val activeColor2 = dynamicColors[(currentIndex + 1) % dynamicColors.size]
    val activeColor3 = dynamicColors[(currentIndex + 2) % dynamicColors.size]

    val currentGlowColor = androidx.compose.ui.graphics.lerp(activeColor1, activeColor2, localFraction)
    val secondaryGlowColor = androidx.compose.ui.graphics.lerp(activeColor2, activeColor3, localFraction)

    Box(
        modifier = modifier
            .scale(scalePulse),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Glowing Soft Ambient & Spot Shadow Canvas
        Canvas(
            modifier = Modifier
                .matchParentSize()
        ) {
            // Layer 1: Outer soft ambient glow with dynamic color
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        currentGlowColor.copy(alpha = 0.45f),
                        secondaryGlowColor.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.maxDimension * 0.9f
                ),
                cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
            )
            // Layer 2: Tight intense glow hugging the border
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        currentGlowColor.copy(alpha = 0.4f),
                        secondaryGlowColor.copy(alpha = 0.4f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Main Button Surface Pill with "Skins" label
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .clickable { onClick() }
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E1430),
                                Color(0xFF0D0A16)
                            )
                        )
                    )
                    .border(
                        width = 1.8.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                currentGlowColor,
                                secondaryGlowColor,
                                currentGlowColor
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(200f * (1f + colorPhase), 100f)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                // Shimmer specular sweep glint
                Canvas(modifier = Modifier.matchParentSize()) {
                    val beamX = size.width * shineProgress
                    drawLine(
                        brush = Brush.linearGradient(
                            0.0f to Color.Transparent,
                            0.5f to Color.White.copy(alpha = 0.5f),
                            1.0f to Color.Transparent,
                            start = Offset(beamX, 0f),
                            end = Offset(beamX + size.width * 0.35f, size.height)
                        ),
                        start = Offset(beamX, 0f),
                        end = Offset(beamX + size.width * 0.35f, size.height),
                        strokeWidth = 12.dp.toPx()
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // High-appeal Cosmetic Sparkle Icon with dynamic glowing tint
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = stringResource(R.string.home_skins_btn),
                        tint = currentGlowColor,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.home_skins_btn),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HeroRatingCard(
    userProfile: UserProfile,
    actualWins: Int,
    actualMatches: Int,
    onClick: () -> Unit = {},
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
            .clickable { onClick() }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_global_ranking_tier).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA0ACCC),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonEmerald.copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_daily_quests_badge),
                            color = NeonEmerald,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonDarkSurface,
                    border = BorderStroke(1.dp, NeonBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.lvl_format, userProfile.level),
                            fontWeight = FontWeight.Black,
                            fontSize = 9.5.sp,
                            color = NeonAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = userProfile.rankTitle.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = NeonCyan
            )

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
                        text = stringResource(R.string.trophies_format, userProfile.trophies),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = stringResource(R.string.xp_format, userProfile.xp % 1000, 1000),
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Win Rate Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonDarkSurface,
                    border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_win_rate_tag, winRate),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonEmerald,
                            maxLines = 1
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
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_total_wins_tag, actualWins),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            maxLines = 1
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
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_streak_tag, userProfile.currentWinStreak),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber,
                            maxLines = 1
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(NeonAmber, Color(0xFFB45309)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎁", fontSize = 18.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_daily_reward_title).uppercase(),
                            fontWeight = FontWeight.Black,
                            fontSize = 10.5.sp,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = if (dailyStreakState.canClaim) stringResource(R.string.home_daily_reward_ready, dailyStreakState.currentDay) else stringResource(R.string.home_daily_reward_streak, dailyStreakState.currentDay),
                            fontSize = 9.5.sp,
                            fontWeight = if (dailyStreakState.canClaim) FontWeight.Bold else FontWeight.Normal,
                            color = if (dailyStreakState.canClaim) NeonAmber else Color(0xFFA0ACCC),
                            maxLines = 1
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
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(NeonMagenta, NeonCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎡", fontSize = 18.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_lucky_wheel_title).uppercase(),
                            fontWeight = FontWeight.Black,
                            fontSize = 10.5.sp,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = if (spinnerState.canSpinToday) stringResource(R.string.home_lucky_wheel_sub_ready) else stringResource(R.string.home_lucky_wheel_sub_done),
                            fontSize = 9.5.sp,
                            fontWeight = if (spinnerState.canSpinToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (spinnerState.canSpinToday) NeonCyan else Color(0xFFA0ACCC),
                            maxLines = 1
                        )
                    }
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
                    text = stringResource(R.string.home_online_arenas_title).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Text(
                text = stringResource(R.string.home_tables_available, arenas.size),
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
                        text = stringResource(arena.titleResId),
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
                            text = stringResource(R.string.home_badge_popular).uppercase(),
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
                            text = stringResource(R.string.home_badge_best_value).uppercase(),
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
                text = stringResource(arena.subtitleResId),
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
                            text = stringResource(R.string.home_arena_entry_fee).uppercase(),
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
                            text = stringResource(R.string.home_arena_win_prize).uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBAC5E1)
                        )
                        Text(
                            text = "🏆 ${arena.winningPrize} ${stringResource(R.string.coins)}",
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
                            text = "${stringResource(R.string.home_arena_play_now)} (🪙 ${arena.entryFee})",
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
                        text = "${stringResource(R.string.home_arena_get_coins)} (🪙 ${arena.entryFee})",
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
                            text = stringResource(R.string.home_offline_ai_title),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.home_offline_ai_subtitle),
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
                    text = stringResource(R.string.home_arena_play_now),
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
fun BottomUtilitySection(
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    // Row with 2 cards: How to Play (Rules) and Match History (Stats)
    Row(
        modifier = modifier.fillMaxWidth(),
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
                        contentDescription = stringResource(R.string.home_how_to_play_title),
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.home_how_to_play_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.home_how_to_play_subtitle),
                        fontSize = 10.sp,
                        color = Color(0xFFA0ACCC),
                        maxLines = 1
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
                        contentDescription = stringResource(R.string.home_match_history_title),
                        tint = NeonAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.home_match_history_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.home_match_history_subtitle),
                        fontSize = 10.sp,
                        color = Color(0xFFA0ACCC),
                        maxLines = 1
                    )
                }
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
