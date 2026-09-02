package com.wallwar.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.wallwar.R
import com.wallwar.data.DailyStreakState
import com.wallwar.data.UserProfile
import com.wallwar.ui.components.DailyStreakRewardsCard
import com.wallwar.ui.theme.NeonAmber
import com.wallwar.ui.theme.NeonBorder
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonDarkBg
import com.wallwar.ui.theme.NeonDarkCard
import com.wallwar.ui.theme.NeonDarkSurface
import com.wallwar.ui.theme.NeonEmerald
import com.wallwar.ui.theme.NeonMagenta

@Composable
fun DailyRewardsScreen(
    userProfile: UserProfile,
    dailyStreakState: DailyStreakState,
    onClaimReward: () -> Unit,
    onNavigateToShop: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(NeonDarkCard)
                                .border(1.dp, NeonBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = stringResource(R.string.daily_rewards_header_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = NeonAmber,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = stringResource(R.string.daily_rewards_7day_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA0ACCC),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Coins Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(NeonDarkCard)
                            .border(1.dp, NeonAmber.copy(alpha = 0.8f), CircleShape)
                            .clickable { onNavigateToShop() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = "🪙", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${userProfile.coins}",
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = stringResource(R.string.coins),
                            tint = NeonAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Streak Status Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                border = BorderStroke(
                    1.5.dp,
                    Brush.horizontalGradient(listOf(NeonAmber, NeonMagenta))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(NeonAmber, Color(0xFFB45309))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column {
                            Text(
                                text = stringResource(R.string.profile_current_streak),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonAmber,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = stringResource(R.string.daily_streak_day_format, dailyStreakState.currentDay),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = if (dailyStreakState.canClaim) stringResource(R.string.daily_streak_claim_ready) else stringResource(R.string.daily_streak_claim_tomorrow),
                                fontSize = 11.sp,
                                color = if (dailyStreakState.canClaim) NeonEmerald else Color(0xFFA0ACCC),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (dailyStreakState.canClaim) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeonAmber
                        ) {
                            Text(
                                text = stringResource(R.string.daily_streak_ready_btn),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeonDarkSurface,
                            border = BorderStroke(1.dp, NeonBorder)
                        ) {
                            Text(
                                text = stringResource(R.string.daily_streak_claimed_pill),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = NeonEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7-Day Streak Rewards Grid Card
            DailyStreakRewardsCard(
                dailyStreakState = dailyStreakState,
                onClaim = onClaimReward,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Information & Streak Rules Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                border = BorderStroke(1.dp, NeonBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.daily_streak_rules_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.daily_streak_rules_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0ACCC),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
