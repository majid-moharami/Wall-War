package com.wallwar.ui.screens.emoji

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wallwar.data.EmojiSkin
import com.wallwar.data.UserProfile
import com.wallwar.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun EmojiShopScreen(
    userProfile: UserProfile,
    allEmojis: List<EmojiSkin>,
    unlockedEmojiIds: Set<String>,
    previewEmoji: EmojiSkin?,
    statusMessage: String?,
    onPreviewEmoji: (EmojiSkin) -> Unit,
    onBuyEmoji: (EmojiSkin) -> Unit,
    onClearStatusMessage: () -> Unit,
    onOpenCoinShop: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(3500)
            onClearStatusMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonDarkBg)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("emoji_shop_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Emoji Skins Chart",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Taunt & Express In Real-Time Duels",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }
            }

            // Coin Balance Badge (Clickable to open Coin Shop)
            Surface(
                shape = CircleShape,
                color = NeonDarkSurface,
                border = BorderStroke(1.dp, NeonAmber),
                modifier = Modifier.clickable { onOpenCoinShop() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = NeonAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${userProfile.coins}",
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonAmber,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Status / Celebration Banner
        AnimatedVisibility(
            visible = statusMessage != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            statusMessage?.let { msg ->
                val isSuccess = msg.contains("Unlocked") || msg.contains("🎉")
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuccess) NeonEmerald.copy(alpha = 0.15f) else NeonMagenta.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSuccess) NeonEmerald else NeonMagenta
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccess) NeonEmerald else Color(0xFFFF8B8B),
                            modifier = Modifier.weight(1f)
                        )
                        if (!isSuccess && msg.contains("more coins")) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onOpenCoinShop,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "Get Coins",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Emote Preview Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(NeonDarkSurface)
                        .border(1.dp, if (previewEmoji != null) NeonCyan else Color(0xFF2B3654), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewEmoji != null) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val scaleAnim by infiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.25f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(350, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        Text(
                            text = previewEmoji.symbol,
                            fontSize = 28.sp,
                            modifier = Modifier.scale(scaleAnim)
                        )
                    } else {
                        Text(
                            text = "💬",
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (previewEmoji != null) "Active Emote Preview" else "Tap Any Emoji to Test",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (previewEmoji != null) NeonCyan else Color.White
                    )
                    Text(
                        text = if (previewEmoji != null) {
                            "Showing ${previewEmoji.symbol} ${previewEmoji.name} (lasts 3s in match)"
                        } else {
                            "Unlocked emojis show over your avatar to opponents in real-time."
                        },
                        fontSize = 11.sp,
                        color = Color(0xFFA0ACCC),
                        lineHeight = 15.sp
                    )
                }

                if (previewEmoji == null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonCyan.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${unlockedEmojiIds.size}/9 Unlocked",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 9 Emoji Skins Chart Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(allEmojis, key = { it.id }) { emoji ->
                val isUnlocked = unlockedEmojiIds.contains(emoji.id)
                val isPreviewing = previewEmoji?.id == emoji.id

                EmojiSkinGridCard(
                    emoji = emoji,
                    isUnlocked = isUnlocked,
                    isPreviewing = isPreviewing,
                    canAfford = userProfile.coins >= emoji.priceCoins,
                    onPreview = { onPreviewEmoji(emoji) },
                    onBuy = { onBuyEmoji(emoji) }
                )
            }
        }
    }
}

@Composable
fun EmojiSkinGridCard(
    emoji: EmojiSkin,
    isUnlocked: Boolean,
    isPreviewing: Boolean,
    canAfford: Boolean,
    onPreview: () -> Unit,
    onBuy: () -> Unit
) {
    val borderColor = when {
        isPreviewing -> NeonCyan
        isUnlocked -> NeonEmerald.copy(alpha = 0.6f)
        canAfford -> NeonAmber.copy(alpha = 0.6f)
        else -> Color(0xFF222B42)
    }

    val cardBg = if (isPreviewing) {
        NeonDarkSurface
    } else {
        NeonDarkCard
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                if (isUnlocked) {
                    onPreview()
                } else {
                    onBuy()
                }
            }
            .testTag("emoji_card_${emoji.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Badge: Unlocked or Tag
            if (isUnlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Unlocked",
                        tint = NeonEmerald,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "OWNED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonEmerald
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFFA0ACCC),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = emoji.tag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA0ACCC),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Emoji Symbol in glowing circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(NeonDarkSurface)
                    .border(
                        1.dp,
                        if (isUnlocked) NeonEmerald.copy(alpha = 0.4f) else Color(0xFF2B3654),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji.symbol,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Emoji Name
            Text(
                text = emoji.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Button / Status
            if (isUnlocked) {
                OutlinedButton(
                    onClick = onPreview,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Test",
                        tint = NeonCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Test",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onBuy,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canAfford) NeonAmber else Color(0xFF2B3654),
                        contentColor = if (canAfford) Color.Black else Color(0xFFA0ACCC)
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .testTag("buy_emoji_${emoji.id}")
                ) {
                    Text(
                        text = "🪙 ${emoji.priceCoins}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
