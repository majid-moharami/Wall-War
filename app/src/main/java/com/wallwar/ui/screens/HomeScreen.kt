package com.wallwar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wallwar.model.AiDifficulty
import com.wallwar.model.GameMode
import com.wallwar.model.OpponentType
import com.wallwar.ui.AppScreen
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
	userProfile: com.wallwar.data.UserProfile = com.wallwar.data.UserProfile(),
	totalWins: Int,
	totalMatches: Int,
	onStartGame: (mode: GameMode, opponent: OpponentType, difficulty: AiDifficulty) -> Unit,
	onNavigate: (AppScreen) -> Unit,
	modifier: Modifier = Modifier
) {
    var selectedAiDifficulty by remember { mutableStateOf(AiDifficulty.NORMAL) }
    var showAiPicker by remember { mutableStateOf(false) }

    val actualWins = totalWins.coerceAtLeast(userProfile.wins)
    val actualMatches = totalMatches.coerceAtLeast(userProfile.totalMatches)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeonDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header Area
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        .border(2.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "WW",
                        color = NeonCyan,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
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
                    .border(1.dp, NeonAmber.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(NeonAmber.copy(alpha = 0.2f)),
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
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // High Rush Hero Section
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
            border = BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(listOf(NeonCyan, NeonMagenta))
            ),
            modifier = Modifier.fillMaxWidth()
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
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Stats Bento Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NeonAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎖️", fontSize = 14.sp)
                    }
                    Column {
                        Text(
                            text = "Rank Tier",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA0ACCC)
                        )
                        Text(
                            text = userProfile.rankTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NeonMagenta.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚔️", fontSize = 14.sp)
                    }
                    Column {
                        Text(
                            text = "Level & XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA0ACCC)
                        )
                        Text(
                            text = "Lvl ${userProfile.level} (${userProfile.xp} XP)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "SELECT BATTLE MODE",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFA0ACCC),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 10.dp)
        )

        // 1. Online Random Multiplayer (Nakama Server)
        Card(
            onClick = {
                onStartGame(GameMode.DUEL, OpponentType.ONLINE, selectedAiDifficulty)
            },
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(NeonEmerald, NeonCyan))),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_online_match")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NeonEmerald.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌐", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Play Random Online Game",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonEmerald)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("LIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                    Text(
                        text = "Nakama Server • 30s Turn Timer • Win +75 Coins",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0ACCC)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = NeonEmerald
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Quick Match Card (Primary Pass & Play)
        Card(
            onClick = {
                onStartGame(GameMode.DUEL, OpponentType.LOCAL_PASS_PLAY, AiDifficulty.NORMAL)
            },
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_quick_match")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Quick Match",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quick Pass & Play",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "2 Players on 1 Phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0ACCC)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = NeonCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Play VS AI Card
        Card(
            onClick = {
                onStartGame(GameMode.DUEL, OpponentType.AI, selectedAiDifficulty)
            },
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, NeonBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_vs_ai")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NeonMagenta.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Play AI",
                            tint = NeonMagenta,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Play VS AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Challenge AI (${selectedAiDifficulty.displayName})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA0ACCC)
                        )
                    }
                    IconButton(onClick = { showAiPicker = !showAiPicker }) {
                        Icon(
                            imageVector = if (showAiPicker) Icons.Default.Tune else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Difficulty options",
                            tint = NeonMagenta
                        )
                    }
                }

                AnimatedVisibility(visible = showAiPicker) {
                    Column(modifier = Modifier.padding(top = 14.dp)) {
                        Text(
                            text = "AI Difficulty Level:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AiDifficulty.values().forEach { diff ->
                                FilterChip(
                                    selected = selectedAiDifficulty == diff,
                                    onClick = {
                                        selectedAiDifficulty = diff
                                        onStartGame(GameMode.DUEL, OpponentType.AI, diff)
                                    },
                                    label = {
                                        Text(
                                            text = diff.displayName,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonMagenta,
                                        selectedLabelColor = Color.White,
                                        containerColor = NeonDarkSurface,
                                        labelColor = Color(0xFFA0ACCC)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Wall Race Mode (9x13 fast board)
        Card(
            onClick = {
                onStartGame(GameMode.RACE, OpponentType.LOCAL_PASS_PLAY, AiDifficulty.NORMAL)
            },
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, NeonBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_wall_race")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NeonAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = "Race Mode",
                        tint = NeonAmber,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wall Race Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "9x13 Board • Race to top with 15 walls",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0ACCC)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = NeonAmber
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Two Column Grid: How to Play & Statistics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                onClick = { onNavigate(AppScreen.RULES) },
                colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonBorder),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_how_to_play")
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
                border = BorderStroke(1.dp, NeonBorder),
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
                        text = "$totalWins W / $totalMatches Matches",
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
            border = BorderStroke(1.dp, NeonBorder),
            modifier = Modifier
                .fillMaxWidth()
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
                Text(
                    text = "Settings & Board Themes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
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
