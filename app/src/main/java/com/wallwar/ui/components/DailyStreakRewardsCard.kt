package com.wallwar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.wallwar.R
import com.wallwar.data.DailyStreakManager
import com.wallwar.data.DailyStreakState
import com.wallwar.ui.theme.NeonAmber
import com.wallwar.ui.theme.NeonBorder
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonDarkCard
import com.wallwar.ui.theme.NeonDarkSurface
import com.wallwar.ui.theme.NeonEmerald
import com.wallwar.ui.theme.NeonMagenta
import com.wallwar.ui.theme.NeonPurple

@Composable
fun DailyStreakRewardsCard(
    dailyStreakState: DailyStreakState,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streak_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val currentDay = dailyStreakState.currentDay
    val canClaim = dailyStreakState.canClaim
    val nextClaimDay = if (canClaim) {
        if (currentDay >= 7) 1 else currentDay + 1
    } else {
        currentDay
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
        border = BorderStroke(
            1.5.dp,
            if (canClaim) Brush.horizontalGradient(listOf(NeonAmber, NeonMagenta, NeonCyan))
            else Brush.horizontalGradient(listOf(NeonBorder, NeonBorder))
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_streak_rewards_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ─── Header: Title & Badges ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(NeonAmber, Color(0xFFD97706)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.daily_rewards_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (canClaim) stringResource(R.string.daily_rewards_claim_hint) else stringResource(R.string.daily_rewards_active_hint),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = if (canClaim) NeonAmber else NeonEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Streak count pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeonDarkSurface,
                    border = BorderStroke(1.dp, if (canClaim) NeonAmber.copy(alpha = glowAlpha) else NeonEmerald)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🔥 $currentDay/7",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = if (canClaim) NeonAmber else NeonEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ─── Grid: Days 1 to 6 (2 rows of 3 columns) ───
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Days 1, 2, 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (day in 1..3) {
                        val reward = DailyStreakManager.rewardForDay(day)
                        val isClaimed = day <= currentDay
                        val isToday = canClaim && (day == nextClaimDay)
                        val isLocked = day > currentDay && !isToday

                        DailyStreakDayItem(
                            dayNumber = day,
                            rewardAmount = reward,
                            isClaimed = isClaimed,
                            isToday = isToday,
                            isLocked = isLocked,
                            isGrandPrize = false,
                            pulseScale = if (isToday) pulseScale else 1f,
                            onItemClick = if (isToday) onClaim else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Row 2: Days 4, 5, 6
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (day in 4..6) {
                        val reward = DailyStreakManager.rewardForDay(day)
                        val isClaimed = day <= currentDay
                        val isToday = canClaim && (day == nextClaimDay)
                        val isLocked = day > currentDay && !isToday

                        DailyStreakDayItem(
                            dayNumber = day,
                            rewardAmount = reward,
                            isClaimed = isClaimed,
                            isToday = isToday,
                            isLocked = isLocked,
                            isGrandPrize = false,
                            pulseScale = if (isToday) pulseScale else 1f,
                            onItemClick = if (isToday) onClaim else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ─── Day 7: The Grand Jackpot Card (Spans Full Width) ───
                val day7Reward = DailyStreakManager.rewardForDay(7) // 500 Coins
                val isDay7Claimed = currentDay >= 7
                val isDay7Today = canClaim && (nextClaimDay == 7)
                val isDay7Locked = !isDay7Claimed && !isDay7Today

                DailyStreakGrandPrizeCard(
                    rewardAmount = day7Reward,
                    isClaimed = isDay7Claimed,
                    isToday = isDay7Today,
                    isLocked = isDay7Locked,
                    pulseScale = if (isDay7Today) pulseScale else 1f,
                    onClaim = if (isDay7Today) onClaim else null
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ─── Primary CTA Claim Button ───
            if (canClaim) {
                Button(
                    onClick = onClaim,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .scale(pulseScale)
                        .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = NeonAmber)
                        .testTag("btn_claim_daily_streak")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.daily_rewards_claim_btn, nextClaimDay, dailyStreakState.todayReward),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NeonDarkSurface,
                    border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.daily_rewards_claimed_msg, currentDay, if (currentDay >= 7) 1 else currentDay + 1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NeonEmerald
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyStreakDayItem(
    dayNumber: Int,
    rewardAmount: Int,
    isClaimed: Boolean,
    isToday: Boolean,
    isLocked: Boolean,
    isGrandPrize: Boolean = false,
    pulseScale: Float = 1f,
    onItemClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardBackground = when {
        isClaimed -> Color(0xFF0F261D) // Dark Emerald tint
        isToday -> Color(0xFF261D0C) // Glowing Amber tint
        else -> NeonDarkSurface // Sleek dark surface
    }

    val cardBorder = when {
        isToday -> BorderStroke(2.dp, NeonAmber)
        isClaimed -> BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.8f))
        else -> BorderStroke(1.dp, Color(0xFF222B3F))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = cardBorder,
        modifier = modifier
            .height(98.dp)
            .scale(if (isToday) pulseScale else 1f)
            .then(if (onItemClick != null) Modifier.clickable { onItemClick() } else Modifier)
            .testTag("streak_day_$dayNumber")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Day Number Badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when {
                    isClaimed -> NeonEmerald.copy(alpha = 0.2f)
                    isToday -> NeonAmber.copy(alpha = 0.25f)
                    else -> Color(0xFF1E2536)
                }
            ) {
                Text(
                    text = stringResource(R.string.daily_rewards_day_n, dayNumber),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        isClaimed -> NeonEmerald
                        isToday -> NeonAmber
                        else -> Color(0xFFA0ACCC)
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Central Reward Visual
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isClaimed -> {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(NeonEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Claimed",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    isToday -> {
                        Text(
                            text = "🪙",
                            fontSize = 22.sp
                        )
                    }
                    else -> {
                        Text(
                            text = "🪙",
                            fontSize = 18.sp,
                            modifier = Modifier.scale(0.85f)
                        )
                    }
                }
            }

            // Reward Amount & Status Subtext
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "+$rewardAmount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = when {
                        isClaimed -> NeonEmerald
                        isToday -> NeonAmber
                        else -> Color.White
                    }
                )
                Text(
                    text = when {
                        isClaimed -> stringResource(R.string.daily_rewards_done)
                        isToday -> stringResource(R.string.daily_rewards_claim_excl)
                        else -> stringResource(R.string.coins)
                    },
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isClaimed -> NeonEmerald.copy(alpha = 0.8f)
                        isToday -> NeonAmber
                        else -> Color(0xFF7685A3)
                    }
                )
            }
        }
    }
}

@Composable
fun DailyStreakGrandPrizeCard(
    rewardAmount: Int,
    isClaimed: Boolean,
    isToday: Boolean,
    isLocked: Boolean,
    pulseScale: Float = 1f,
    onClaim: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bgBrush = when {
        isClaimed -> Brush.horizontalGradient(
            listOf(Color(0xFF0D291D), Color(0xFF0F3626), Color(0xFF0D291D))
        )
        isToday -> Brush.horizontalGradient(
            listOf(Color(0xFF3B2706), Color(0xFF593C09), Color(0xFF3B2706))
        )
        else -> Brush.horizontalGradient(
            listOf(Color(0xFF1E1C2E), Color(0xFF2B2240), Color(0xFF1E1C2E))
        )
    }

    val borderBrush = when {
        isToday -> Brush.horizontalGradient(listOf(NeonAmber, Color(0xFFFFDF00), NeonMagenta))
        isClaimed -> Brush.horizontalGradient(listOf(NeonEmerald, NeonCyan))
        else -> Brush.horizontalGradient(listOf(NeonPurple, NeonBorder))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(if (isToday) 2.dp else 1.5.dp, borderBrush),
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .scale(if (isToday) pulseScale else 1f)
            .then(if (onClaim != null) Modifier.clickable { onClaim() } else Modifier)
            .testTag("streak_day_7_grand_prize")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Crown Icon & Day 7 Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    if (isToday) listOf(Color(0xFFFFD700), NeonAmber)
                                    else if (isClaimed) listOf(NeonEmerald, Color(0xFF047857))
                                    else listOf(NeonPurple, Color(0xFF4C1D95))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isClaimed) "✓" else "👑",
                            fontSize = if (isClaimed) 24.sp else 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isClaimed) Color.Black else Color.Unspecified
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isToday) NeonAmber.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = stringResource(R.string.daily_rewards_day7_title),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isToday) NeonAmber else if (isClaimed) NeonEmerald else Color(0xFFFFD700),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.daily_rewards_day7_desc),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA0ACCC)
                        )
                    }
                }

                // Right: Big Reward Pill & Status
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "+$rewardAmount 🪙",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isClaimed) NeonEmerald else if (isToday) Color(0xFFFFD700) else NeonAmber
                    )
                    Text(
                        text = when {
                            isClaimed -> stringResource(R.string.daily_rewards_status_claimed)
                            isToday -> stringResource(R.string.daily_rewards_status_tap_claim)
                            else -> stringResource(R.string.daily_rewards_status_locked)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isClaimed -> NeonEmerald
                            isToday -> NeonAmber
                            else -> Color(0xFF7685A3)
                        }
                    )
                }
            }
        }
    }
}
