package com.wallwar.ui.screens.profile

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wallwar.data.ProfileSkin
import com.wallwar.data.ProfileSkinCatalog
import com.wallwar.data.UserProfile
import com.wallwar.data.nakama.NakamaFriend
import com.wallwar.ui.components.AvatarBadge
import com.wallwar.ui.theme.NeonAmber
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonDarkBg
import com.wallwar.ui.theme.NeonDarkCard
import com.wallwar.ui.theme.NeonDarkSurface
import com.wallwar.ui.theme.NeonEmerald
import com.wallwar.ui.theme.NeonMagenta

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    signInStatus: String?,
    friends: List<NakamaFriend> = emptyList(),
    unlockedAvatarSkinIds: Set<String> = ProfileSkinCatalog.DEFAULT_UNLOCKED_SKIN_IDS,
    allProfileSkins: List<ProfileSkin> = ProfileSkinCatalog.ALL_SKINS,
    savedGooglePhotoUrl: String? = null,
    onUnlockSkin: (ProfileSkin, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onEquipSkin: (ProfileSkin) -> Unit = {},
    onEquipGoogleAvatar: () -> Unit = {},
    onSignInWithGoogle: (Context) -> Unit,
    onClearSignInStatus: () -> Unit,
    onSignOut: (Context) -> Unit,
    onAddFriend: (String, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onRemoveFriend: (String) -> Unit = {},
    onChallengeFriend: (String) -> Unit = {},
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCoinShop: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var friendSearchQuery by remember { mutableStateOf("") }
    var addFriendStatus by remember { mutableStateOf<String?>(null) }
    var skinActionFeedback by remember { mutableStateOf<String?>(null) }

    val activeSkin = ProfileSkinCatalog.getSkinById(userProfile.photoUrl)
    val isGoogleAvatarEquipped = !userProfile.photoUrl.isNullOrBlank() &&
            (userProfile.photoUrl.startsWith("http://") || userProfile.photoUrl.startsWith("https://"))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeonDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Status / Banner Feedback
        val bannerMessage = skinActionFeedback ?: signInStatus
        if (!bannerMessage.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (bannerMessage.contains("error", ignoreCase = true) || 
                        bannerMessage.contains("failed", ignoreCase = true) ||
                        bannerMessage.contains("Not enough", ignoreCase = true)) {
                        Color(0xFF3E1A24)
                    } else {
                        NeonDarkSurface
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (bannerMessage.contains("error", ignoreCase = true) || 
                        bannerMessage.contains("failed", ignoreCase = true) ||
                        bannerMessage.contains("Not enough", ignoreCase = true)) NeonMagenta else NeonCyan
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Status",
                        tint = if (bannerMessage.contains("error", ignoreCase = true) || 
                            bannerMessage.contains("failed", ignoreCase = true) ||
                            bannerMessage.contains("Not enough", ignoreCase = true)) NeonMagenta else NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = bannerMessage,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            skinActionFeedback = null
                            onClearSignInStatus()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0xFFA0ACCC)
                        )
                    }
                }
            }
        }

        // Profile Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
            border = BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(listOf(NeonCyan, NeonMagenta))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar with Universal AvatarBadge
                AvatarBadge(
                    photoUrl = userProfile.photoUrl,
                    size = 88.dp,
                    borderWidth = 3.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = userProfile.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = userProfile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA0ACCC)
                )

                // Currently Equipped Avatar Pill
                if (activeSkin != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(activeSkin.primaryColorHex).copy(alpha = 0.15f))
                            .border(1.dp, Color(activeSkin.primaryColorHex).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${activeSkin.symbol} ${activeSkin.name} (${activeSkin.tag})",
                            color = Color(activeSkin.primaryColorHex),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isGoogleAvatarEquipped) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF4285F4).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF4285F4).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "🌐 Google Account Photo Equipped",
                            color = Color(0xFF8AB4F8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Coins Header Pill (🪙 +75 Coins per Win)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2411))
                        .border(1.dp, NeonAmber, RoundedCornerShape(12.dp))
                        .clickable { onNavigateToCoinShop() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = NeonAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${userProfile.coins} Coins",
                        color = NeonAmber,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(+75 / Win)",
                        color = Color(0xFFE2C275),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Verification Badge Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (userProfile.isLoggedIn) Color(0xFF003828) else Color(0xFF262A3E))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (userProfile.isLoggedIn) Icons.Default.VerifiedUser else Icons.Default.Shield,
                        contentDescription = "Status",
                        tint = if (userProfile.isLoggedIn) NeonEmerald else Color(0xFFA0ACCC),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (userProfile.isLoggedIn) "Google Verified" else "Guest Duelist",
                        color = if (userProfile.isLoggedIn) NeonEmerald else Color(0xFFA0ACCC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Official Sign in with Google / Email / Logout Button
                if (!userProfile.isLoggedIn) {
                    Button(
                        onClick = onNavigateToAuth,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "Log In or Create Account",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { onSignInWithGoogle(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color(0xFF4285F4)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            onSignOut(context)
                            onNavigateToAuth()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonMagenta),
                        border = BorderStroke(1.dp, NeonMagenta)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sign Out",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Sign Out from Nakama", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Level & XP Progress Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard)
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
                    Text(
                        text = "LEVEL ${userProfile.level}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonCyan
                    )
                    Text(
                        text = userProfile.rankTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonMagenta,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (userProfile.xp % 100) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = NeonCyan,
                    trackColor = NeonDarkSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${userProfile.xp % 100} / 100 XP to next level",
                        fontSize = 11.sp,
                        color = Color(0xFFA0ACCC)
                    )
                    Text(
                        text = "${userProfile.trophies} Trophies",
                        fontSize = 11.sp,
                        color = NeonAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Win Streaks Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🔥 ${userProfile.currentWinStreak}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonAmber
                    )
                    Text(
                        text = "Current Streak",
                        fontSize = 12.sp,
                        color = Color(0xFFA0ACCC)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(NeonDarkSurface)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⚡ ${userProfile.longestWinStreak}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonCyan
                    )
                    Text(
                        text = "Best Streak",
                        fontSize = 12.sp,
                        color = Color(0xFFA0ACCC)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nakama Friends Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Friends",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NAKAMA FRIENDS (${friends.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Add Friend input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = friendSearchQuery,
                        onValueChange = { friendSearchQuery = it },
                        placeholder = { Text("Friend's username", fontSize = 13.sp, color = Color(0xFF6B7280)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF2E334D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (friendSearchQuery.isNotBlank()) {
                                addFriendStatus = "Adding..."
                                onAddFriend(friendSearchQuery.trim()) { success ->
                                    addFriendStatus = if (success) "Friend added!" else "User not found"
                                    if (success) friendSearchQuery = ""
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Add", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (!addFriendStatus.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = addFriendStatus ?: "",
                        fontSize = 12.sp,
                        color = if (addFriendStatus?.contains("added", ignoreCase = true) == true) NeonEmerald else NeonMagenta
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (friends.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonDarkSurface)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No friends added yet. Enter a username above to duel them online!",
                            color = Color(0xFFA0ACCC),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    friends.forEach { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonDarkSurface)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = friend.displayName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = friend.displayName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Level ${friend.level} • ${friend.trophies} Trophies",
                                    fontSize = 11.sp,
                                    color = Color(0xFFA0ACCC)
                                )
                            }
                            Button(
                                onClick = { onChallengeFriend(friend.username) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Duel",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Duel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats Summary
        Text(
            text = "BATTLE STATISTICS",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFA0ACCC),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Total Matches",
                value = "${userProfile.totalMatches}",
                color = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Victories",
                value = "${userProfile.wins}",
                color = NeonEmerald,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Walls Placed",
                value = "${userProfile.wallsPlaced}",
                color = NeonMagenta,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Navigation Buttons
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard)
        ) {
            Column {
                ProfileOptionRow(
                    icon = Icons.Default.History,
                    title = "Match History",
                    subtitle = "Review past duel records and stats",
                    iconColor = NeonCyan,
                    onClick = onNavigateToHistory
                )
                ProfileOptionRow(
                    icon = Icons.Default.Settings,
                    title = "Settings & Sound",
                    subtitle = "Board theme, audio effects & vibration",
                    iconColor = NeonAmber,
                    onClick = onNavigateToSettings
                )
                ProfileOptionRow(
                    icon = Icons.Default.Backup,
                    title = "Data Backup & Restore",
                    subtitle = "Cloud sync, JSON backup & restore data",
                    iconColor = NeonEmerald,
                    onClick = onNavigateToSettings
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NeonDarkCard)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                fontSize = 20.sp
            )
            Text(
                text = title,
                color = Color(0xFFA0ACCC),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ProfileOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                color = Color(0xFFA0ACCC),
                fontSize = 12.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Go",
            tint = Color(0xFFA0ACCC),
            modifier = Modifier.size(18.dp)
        )
    }
}
