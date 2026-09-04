package com.wallwar.ui.screens.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wallwar.data.UserProfile
import com.wallwar.ui.theme.NeonAmber
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonDarkBg
import com.wallwar.ui.theme.NeonDarkCard
import com.wallwar.ui.theme.NeonDarkSurface
import com.wallwar.ui.theme.NeonEmerald
import com.wallwar.ui.theme.NeonMagenta
import com.wallwar.ui.theme.NeonPurple
import androidx.compose.ui.res.stringResource
import com.wallwar.R

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.style.TextAlign

@Composable
fun RankingScreen(
    userProfile: UserProfile,
    leaderboard: List<LeaderboardPlayer>,
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!userProfile.isLoggedIn) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(NeonDarkBg)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.12f))
                    .border(2.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.ranking_arena_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ranking_sign_in_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA0ACCC),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNavigateToProfile,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.ranking_sign_in_btn),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        return
    }

    val currentUserPlayer = leaderboard.firstOrNull { it.isUser }
    val isUserInTop5 = currentUserPlayer != null && currentUserPlayer.rank <= 5

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeonDarkBg)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.ranking_global_leaderboard),
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = stringResource(R.string.ranking_arena_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier.size(40.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.ranking_refresh_content_desc),
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(NeonMagenta, NeonPurple)))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = stringResource(R.string.ranking_season_1),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.ranking_season_1),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Show horizontal loading indicator banner when refreshing/syncing
        if (isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonDarkCard)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = NeonCyan,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.ranking_syncing),
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Top 3 Podium
        if (leaderboard.size >= 3) {
            PodiumSection(
                p1 = leaderboard.firstOrNull { it.rank == 1 } ?: leaderboard[0],
                p2 = leaderboard.firstOrNull { it.rank == 2 } ?: leaderboard[1],
                p3 = leaderboard.firstOrNull { it.rank == 3 } ?: leaderboard[2]
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.ranking_all_duelists),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFA0ACCC),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Leaderboard List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(leaderboard, key = { if (it.id.isNotBlank()) "${it.id}_${it.rank}" else "${it.rank}_${it.name}" }) { player ->
                LeaderboardCard(player = player)
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Show player rank at bottom of screen if not in top 5
        if (currentUserPlayer != null && !isUserInTop5) {
            UserBottomRankCard(player = currentUserPlayer)
        }
    }
}

@Composable
private fun UserBottomRankCard(player: LeaderboardPlayer) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2642)),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            brush = Brush.horizontalGradient(listOf(NeonCyan, NeonPurple, NeonMagenta))
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ranking_your_rank_label),
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "#${player.rank}",
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonCyan,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = "${player.trophies} 🏆",
                    fontWeight = FontWeight.Bold,
                    color = NeonAmber,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NeonDarkSurface)
                        .border(1.5.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!player.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = player.avatarUrl,
                            contentDescription = player.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = player.name,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name & Tier
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = player.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonCyan)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.ranking_you_badge),
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(player.titleResId),
                            color = Color(0xFFA0ACCC),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.ranking_lvl_format, player.level),
                            color = NeonEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Wins & Win Rate
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${player.wins} W",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = stringResource(R.string.ranking_win_rate_format, player.winRate),
                        color = Color(0xFFA0ACCC),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PodiumSection(
    p1: LeaderboardPlayer,
    p2: LeaderboardPlayer,
    p3: LeaderboardPlayer
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place
        PodiumCard(
            player = p2,
            rankColor = Color(0xFFC0C0C0),
            heightDp = 130,
            modifier = Modifier.weight(1f)
        )

        // 1st Place
        PodiumCard(
            player = p1,
            rankColor = NeonAmber,
            heightDp = 160,
            modifier = Modifier.weight(1f)
        )

        // 3rd Place
        PodiumCard(
            player = p3,
            rankColor = Color(0xFFCD7F32),
            heightDp = 115,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PodiumCard(
    player: LeaderboardPlayer,
    rankColor: Color,
    heightDp: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
        border = androidx.compose.foundation.BorderStroke(
            width = if (player.isUser) 2.dp else 1.dp,
            color = if (player.isUser) NeonCyan else rankColor.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(2.dp, rankColor, CircleShape)
                    .background(NeonDarkSurface),
                contentAlignment = Alignment.Center
            ) {
                if (!player.avatarUrl.isNull_or_empty()) {
                    AsyncImage(
                        model = player.avatarUrl,
                        contentDescription = player.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = player.name,
                        tint = rankColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "#${player.rank}",
                fontWeight = FontWeight.ExtraBold,
                color = rankColor,
                fontSize = 14.sp
            )

            Text(
                text = player.name,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1
            )

            Text(
                text = "${player.trophies} 🏆",
                fontWeight = FontWeight.SemiBold,
                color = NeonAmber,
                fontSize = 10.sp
            )
        }
    }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()

@Composable
private fun LeaderboardCard(player: LeaderboardPlayer) {
    val borderColor = if (player.isUser) NeonCyan else Color.Transparent
    val cardBg = if (player.isUser) Color(0xFF1F2642) else NeonDarkCard

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = if (player.isUser) androidx.compose.foundation.BorderStroke(1.5.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Number
            Box(
                modifier = Modifier
                    .width(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${player.rank}",
                    fontWeight = FontWeight.ExtraBold,
                    color = when (player.rank) {
                        1 -> NeonAmber
                        2 -> Color(0xFFC0C0C0)
                        3 -> Color(0xFFCD7F32)
                        else -> Color(0xFFA0ACCC)
                    },
                    fontSize = 14.sp
                )
            }

            // Avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(NeonDarkSurface)
                    .border(1.dp, if (player.isUser) NeonCyan else Color(0xFF2E375A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!player.avatarUrl.isNull_or_empty()) {
                    AsyncImage(
                        model = player.avatarUrl,
                        contentDescription = player.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = player.name,
                        tint = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Player Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = player.name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    if (player.isUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonCyan)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.ranking_you_badge),
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(player.titleResId),
                        color = Color(0xFFA0ACCC),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ranking_lvl_format, player.level),
                        color = NeonEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Trophy & Win Rate
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${player.trophies} 🏆",
                    fontWeight = FontWeight.Bold,
                    color = NeonAmber,
                    fontSize = 13.sp
                )
                Text(
                    text = stringResource(R.string.ranking_win_rate_format, player.winRate),
                    color = Color(0xFFA0ACCC),
                    fontSize = 10.sp
                )
            }
        }
    }
}
