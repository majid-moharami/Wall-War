package com.wallwar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import com.wallwar.R
import com.wallwar.data.Arena
import com.wallwar.model.AiDifficulty
import com.wallwar.model.OpponentType
import com.wallwar.ui.theme.NeonAmber
import com.wallwar.ui.theme.NeonBorder
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonDarkCard
import com.wallwar.ui.theme.NeonDarkSurface
import com.wallwar.ui.theme.NeonEmerald
import com.wallwar.ui.theme.NeonPurple

@Composable
fun OfflinePlayDialog(
    userCoins: Int,
    offlineArena: Arena,
    isAdPlaying: Boolean = false,
    isRewardedAdLoading: Boolean = false,
    onStartOfflineMatch: (opponentType: OpponentType, difficulty: AiDifficulty, useAd: Boolean) -> Unit,
    onOpenShop: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOpponent by remember { mutableStateOf(OpponentType.AI) }
    var selectedDifficulty by remember { mutableStateOf(AiDifficulty.NORMAL) }
    val entryFee = offlineArena.entryFee
    val hasEnoughCoins = userCoins >= entryFee
    val sectionColor = Color(offlineArena.colorHex)

    Dialog(
        onDismissRequest = {
            if (!isRewardedAdLoading && !isAdPlaying) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isRewardedAdLoading && !isAdPlaying,
            dismissOnClickOutside = !isRewardedAdLoading && !isAdPlaying
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
            border = BorderStroke(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(sectionColor, NeonPurple.copy(alpha = 0.5f), NeonDarkSurface)
                )
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("offline_play_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Title & Close Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(sectionColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = sectionColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.offline_practice_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.offline_practice_subtitle),
                                fontSize = 11.sp,
                                color = Color(0xFFA0ACCC)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (!isRewardedAdLoading && !isAdPlaying) {
                                onDismiss()
                            }
                        },
                        enabled = !isRewardedAdLoading && !isAdPlaying,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.btn_close),
                            tint = if (isRewardedAdLoading || isAdPlaying) Color(0xFF4A5568) else Color(0xFFA0ACCC)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Tabs (VS AI vs Local Pass & Play)
                Text(
                    text = stringResource(R.string.offline_select_opponent),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFA0ACCC),
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonDarkSurface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selectedOpponent == OpponentType.AI) sectionColor.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .clickable(enabled = !isRewardedAdLoading && !isAdPlaying) {
                                selectedOpponent = OpponentType.AI
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.offline_mode_ai_bot),
                            fontSize = 13.sp,
                            fontWeight = if (selectedOpponent == OpponentType.AI) FontWeight.Black else FontWeight.Bold,
                            color = if (selectedOpponent == OpponentType.AI) Color.White else Color(0xFF6B7A99)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selectedOpponent == OpponentType.LOCAL_PASS_PLAY) sectionColor.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .clickable(enabled = !isRewardedAdLoading && !isAdPlaying) {
                                selectedOpponent = OpponentType.LOCAL_PASS_PLAY
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.offline_mode_pass_and_play),
                            fontSize = 13.sp,
                            fontWeight = if (selectedOpponent == OpponentType.LOCAL_PASS_PLAY) FontWeight.Black else FontWeight.Bold,
                            color = if (selectedOpponent == OpponentType.LOCAL_PASS_PLAY) Color.White else Color(0xFF6B7A99)
                        )
                    }
                }

                // AI Difficulty Selector (if VS AI is selected)
                if (selectedOpponent == OpponentType.AI) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.offline_ai_diff_title),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFA0ACCC),
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiDifficulty.entries.forEach { diff ->
                            val isSelected = selectedDifficulty == diff
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) sectionColor.copy(alpha = 0.25f)
                                        else NeonDarkSurface
                                    )
                                    .clickable(enabled = !isRewardedAdLoading && !isAdPlaying) {
                                        selectedDifficulty = diff
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val diffName = when (diff) {
                                    AiDifficulty.EASY -> stringResource(R.string.difficulty_easy)
                                    AiDifficulty.NORMAL -> stringResource(R.string.difficulty_normal)
                                    AiDifficulty.PRO -> stringResource(R.string.difficulty_hard)
                                }
                                Text(
                                    text = diffName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) sectionColor else Color(0xFFA0ACCC)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Info banner: Practice rules
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NeonDarkSurface,
                    border = BorderStroke(1.dp, Color(0xFF222B42)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🛡️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.offline_practice_rules_note),
                            fontSize = 11.sp,
                            color = Color(0xFFBAC5E1),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action 1: Pay 25 Coins
                if (hasEnoughCoins) {
                    Button(
                        onClick = {
                            onDismiss()
                            onStartOfflineMatch(selectedOpponent, selectedDifficulty, false)
                        },
                        enabled = !isRewardedAdLoading && !isAdPlaying,
                        colors = ButtonDefaults.buttonColors(containerColor = sectionColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("offline_play_pay_coins_btn")
                    ) {
                        Text(
                            text = stringResource(R.string.offline_play_coins_btn, entryFee),
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenShop()
                        },
                        enabled = !isRewardedAdLoading && !isAdPlaying,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonAmber),
                        border = BorderStroke(1.dp, NeonAmber),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.offline_need_coins_btn, entryFee),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action 2: Free Entry via Video Ad
                Button(
                    onClick = {
                        // Do not dismiss immediately; keep dialog open so loading is visible until ad plays!
                        onStartOfflineMatch(selectedOpponent, selectedDifficulty, true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRewardedAdLoading || isAdPlaying) NeonDarkSurface else NeonDarkSurface,
                        contentColor = NeonCyan
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isRewardedAdLoading || isAdPlaying) NeonCyan else NeonCyan.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isAdPlaying && !isRewardedAdLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("offline_play_ad_btn")
                ) {
                    if (isRewardedAdLoading || isAdPlaying) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAdPlaying) stringResource(R.string.offline_ad_playing) else stringResource(R.string.offline_ad_loading),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.offline_free_entry_ad),
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
