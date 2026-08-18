package com.wallwar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wallwar.data.DailyMission
import com.wallwar.data.UserProfile
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
fun DailyQuestsScreen(
    userProfile: UserProfile,
    dailyMissions: List<DailyMission>,
    onClaimMissionReward: (String) -> Unit,
    onNavigateToShop: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = dailyMissions.size
    val completedCount = dailyMissions.count { it.isCompleted }
    val claimedCount = dailyMissions.count { it.isClaimed }
    val claimableCount = dailyMissions.count { it.isCompleted && !it.isClaimed }
    val allCompleted = totalCount > 0 && completedCount == totalCount

    val progressFraction = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "DailyQuestsGlow")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulse"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NeonDarkBg,
        topBar = {
            Surface(
                color = NeonDarkSurface,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(NeonDarkCard)
                                .border(1.dp, NeonBorder, CircleShape)
                                .testTag("daily_quests_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "DAILY QUESTS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Earn Coins & XP Daily",
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                        }
                    }

                    // Coins Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(NeonDarkCard)
                            .border(1.dp, NeonAmber.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                            .clickable { onNavigateToShop() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("quests_coins_pill")
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
                            contentDescription = "Buy Coins",
                            tint = NeonAmber,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Overview Progress Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(
                            listOf(
                                NeonCyan.copy(alpha = if (claimableCount > 0) alphaPulse else 0.5f),
                                NeonPurple.copy(alpha = 0.4f),
                                NeonAmber.copy(alpha = 0.5f)
                            )
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(22.dp),
                            spotColor = NeonCyan.copy(alpha = 0.3f),
                            ambientColor = NeonPurple.copy(alpha = 0.2f)
                        )
                        .testTag("quests_overview_card")
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
                                    text = "TODAY'S MISSION PROGRESS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonCyan,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$completedCount of $totalCount Completed",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            // Claim All button if multiple claimable
                            if (claimableCount > 1) {
                                Button(
                                    onClick = {
                                        dailyMissions.filter { it.isCompleted && !it.isClaimed }.forEach {
                                            onClaimMissionReward(it.id)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("claim_all_quests_btn")
                                ) {
                                    Text(
                                        text = "Claim All ($claimableCount)",
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black,
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NeonDarkSurface,
                                    border = BorderStroke(1.dp, NeonBorder)
                                ) {
                                    Text(
                                        text = "🔄 Resets 00:00 UTC",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA0ACCC),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFF1B2234))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction.coerceAtLeast(0.04f))
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(NeonCyan, NeonPurple, NeonAmber)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (allCompleted) "🔥 All Daily Missions Completed!" else "Complete missions to earn coin rewards & XP boosts.",
                                fontSize = 11.sp,
                                color = if (allCompleted) NeonAmber else Color(0xFFA0ACCC),
                                fontWeight = if (allCompleted) FontWeight.Bold else FontWeight.Normal
                            )

                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE MISSIONS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "$claimedCount / $totalCount Claimed",
                        fontSize = 11.sp,
                        color = Color(0xFFA0ACCC),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 2. Mission Items List
            if (dailyMissions.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                        border = BorderStroke(1.dp, NeonBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🎯", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Missions Loading...",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your daily tactical challenges are generating.",
                                fontSize = 12.sp,
                                color = Color(0xFFA0ACCC),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(dailyMissions, key = { it.id }) { mission ->
                    DailyMissionCard(
                        mission = mission,
                        onClaim = { onClaimMissionReward(mission.id) }
                    )
                }
            }

            // 3. Pro Tips / Milestone Notice Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonDarkSurface),
                    border = BorderStroke(1.dp, Color(0xFF222B42)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Quest Guidelines",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Daily missions reset every midnight UTC. Completed matches in Online Arena, AI Bot, and Pass & Play count towards your progress!",
                                fontSize = 11.sp,
                                color = Color(0xFFA0ACCC),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DailyMissionCard(
    mission: DailyMission,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReadyToClaim = mission.isCompleted && !mission.isClaimed
    val isFinished = mission.isClaimed
    val progress = (mission.currentProgress.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReadyToClaim) NeonDarkSurface else NeonDarkCard
        ),
        border = BorderStroke(
            width = if (isReadyToClaim) 1.5.dp else 1.dp,
            color = when {
                isReadyToClaim -> NeonEmerald
                isFinished -> NeonBorder.copy(alpha = 0.6f)
                else -> NeonBorder
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("mission_item_${mission.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Icon & Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isReadyToClaim -> NeonEmerald.copy(alpha = 0.2f)
                                    isFinished -> Color(0xFF1E2435)
                                    else -> NeonCyan.copy(alpha = 0.15f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = mission.icon, fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = mission.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isFinished) Color(0xFFA0ACCC) else Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = mission.description,
                            fontSize = 11.sp,
                            color = Color(0xFFA0ACCC)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Reward Tag Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonDarkSurface)
                        .border(1.dp, NeonAmber.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🪙 +${mission.coinReward}",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = NeonAmber
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "⚡+${mission.xpReward}XP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar and Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Progress Bar and Text
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isFinished) "Claimed" else "${mission.currentProgress} / ${mission.target}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isFinished -> NeonEmerald
                                isReadyToClaim -> NeonEmerald
                                else -> Color(0xFFA0ACCC)
                            }
                        )

                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = Color(0xFFA0ACCC)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF181E2E))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceAtLeast(0.02f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    when {
                                        isFinished -> Brush.horizontalGradient(listOf(NeonEmerald, NeonCyan))
                                        isReadyToClaim -> Brush.horizontalGradient(listOf(NeonEmerald, NeonAmber))
                                        else -> Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Action state: Claim / Completed / In Progress
                when {
                    isReadyToClaim -> {
                        Button(
                            onClick = onClaim,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("claim_mission_${mission.id}")
                        ) {
                            Text(
                                text = "Claim 🪙",
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                    isFinished -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF132223),
                            border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Done",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonEmerald
                                )
                            }
                        }
                    }
                    else -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeonDarkSurface,
                            border = BorderStroke(1.dp, Color(0xFF222B42))
                        ) {
                            Text(
                                text = "In Progress",
                                fontSize = 11.sp,
                                color = Color(0xFF8B98B5),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
