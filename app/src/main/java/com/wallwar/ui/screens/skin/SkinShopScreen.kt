package com.wallwar.ui.screens.skin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wallwar.data.BallSkin
import com.wallwar.data.EmojiSkin
import com.wallwar.data.ProfileSkin
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
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SkinShopScreen(
    userProfile: UserProfile,
    allBallSkins: List<BallSkin>,
    unlockedBallSkinIds: Set<String>,
    equippedBallSkinId: String,
    allEmojis: List<EmojiSkin>,
    unlockedEmojiIds: Set<String>,
    allProfileSkins: List<ProfileSkin>,
    unlockedAvatarSkinIds: Set<String>,
    selectedTab: Int,
    previewBallSkin: BallSkin?,
    previewEmoji: EmojiSkin?,
    previewProfileSkin: ProfileSkin?,
    statusMessage: String?,
    insufficientCoinsInfo: SkinShopViewModel.InsufficientCoinsInfo? = null,
    onDismissInsufficientCoinsDialog: () -> Unit = {},
    onSelectTab: (Int) -> Unit,
    onPreviewBallSkin: (BallSkin) -> Unit,
    onClearBallPreview: () -> Unit,
    onBuyBallSkin: (BallSkin) -> Unit,
    onEquipBallSkin: (BallSkin) -> Unit,
    onPreviewEmoji: (EmojiSkin) -> Unit,
    onClearEmojiPreview: () -> Unit,
    onBuyEmoji: (EmojiSkin) -> Unit,
    onPreviewProfileSkin: (ProfileSkin) -> Unit,
    onClearProfileSkinPreview: () -> Unit,
    onBuyProfileSkin: (ProfileSkin) -> Unit,
    onEquipProfileSkin: (ProfileSkin) -> Unit,
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

    val tabs = listOf("⚽ Ball Skins", "😄 Emojis", "👤 Profile Skins")

    // Insufficient Coins Dialog with direct link to Coin Shop
    if (insufficientCoinsInfo != null) {
        val numberFormatter = NumberFormat.getNumberInstance(Locale.US)
        AlertDialog(
            onDismissRequest = onDismissInsufficientCoinsDialog,
            containerColor = NeonDarkCard,
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🪙", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Need More Coins!",
                        color = NeonAmber,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "You need ${numberFormatter.format(insufficientCoinsInfo.shortage)} more coins to unlock '${insufficientCoinsInfo.itemName}' (Price: 🪙 ${numberFormatter.format(insufficientCoinsInfo.price)}).",
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "You can purchase coins in the Coins Store and return to claim your skin right away!",
                        color = Color(0xFFA0ACCC),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissInsufficientCoinsDialog()
                        onOpenCoinShop()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🪙 Open Coins Store", color = Color.Black, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismissInsufficientCoinsDialog,
                    border = BorderStroke(1.dp, NeonBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    // Ball Preview Dialog
    if (previewBallSkin != null) {
        BallSkinDetailDialog(
            skin = previewBallSkin,
            isUnlocked = previewBallSkin.isFree || unlockedBallSkinIds.contains(previewBallSkin.id),
            isEquipped = equippedBallSkinId == previewBallSkin.id,
            userCoins = userProfile.coins,
            userLevel = userProfile.level,
            onBuy = { onBuyBallSkin(previewBallSkin) },
            onEquip = { onEquipBallSkin(previewBallSkin) },
            onDismiss = onClearBallPreview
        )
    }

    // Emoji Preview Dialog
    if (previewEmoji != null) {
        EmojiDetailDialog(
            emoji = previewEmoji,
            isUnlocked = previewEmoji.isDefaultUnlocked || unlockedEmojiIds.contains(previewEmoji.id),
            userCoins = userProfile.coins,
            onBuy = { onBuyEmoji(previewEmoji) },
            onDismiss = onClearEmojiPreview
        )
    }

    // Profile Skin Preview Dialog
    if (previewProfileSkin != null) {
        ProfileSkinDetailDialog(
            skin = previewProfileSkin,
            isUnlocked = previewProfileSkin.isDefault || unlockedAvatarSkinIds.contains(previewProfileSkin.id),
            isEquipped = userProfile.photoUrl == "skin:${previewProfileSkin.id}" || (previewProfileSkin.isDefault && (userProfile.photoUrl == null || userProfile.photoUrl == "skin:skin_default")),
            userCoins = userProfile.coins,
            onBuy = { onBuyProfileSkin(previewProfileSkin) },
            onEquip = { onEquipProfileSkin(previewProfileSkin) },
            onDismiss = onClearProfileSkinPreview
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonDarkBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. Top Header Row: Back Button + Title + Coins Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("skin_shop_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Skin Vault & Armory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Level ${userProfile.level} Duelist Armory",
                        fontSize = 11.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Coin Pill Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(NeonDarkSurface)
                    .border(1.dp, NeonAmber.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .clickable { onOpenCoinShop() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("skin_shop_coins_pill")
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
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Category Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = NeonDarkSurface,
            contentColor = NeonCyan,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = NeonCyan
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, NeonBorder, RoundedCornerShape(12.dp))
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Tab(
                    selected = isSelected,
                    onClick = { onSelectTab(index) },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isSelected) NeonCyan else Color(0xFF8E9CBF)
                        )
                    },
                    modifier = Modifier.testTag("skin_tab_$index")
                )
            }
        }

        // Status Toast
        AnimatedVisibility(
            visible = statusMessage != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            if (statusMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                    border = BorderStroke(1.dp, NeonMagenta),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusMessage,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Content Grids based on Selected Tab
        when (selectedTab) {
            0 -> {
                // Ball Skins Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("ball_skins_grid"),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allBallSkins, key = { it.id }) { ball ->
                        val isUnlocked = ball.isFree || unlockedBallSkinIds.contains(ball.id)
                        val isEquipped = equippedBallSkinId == ball.id

                        BallSkinCard(
                            skin = ball,
                            isUnlocked = isUnlocked,
                            isEquipped = isEquipped,
                            userLevel = userProfile.level,
                            onPreview = { onPreviewBallSkin(ball) },
                            onBuy = { onBuyBallSkin(ball) },
                            onEquip = { onEquipBallSkin(ball) }
                        )
                    }
                }
            }
            1 -> {
                // Emoji Emotes Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("emoji_skins_grid"),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allEmojis, key = { it.id }) { emoji ->
                        val isUnlocked = emoji.isDefaultUnlocked || unlockedEmojiIds.contains(emoji.id)

                        EmojiSkinCard(
                            emoji = emoji,
                            isUnlocked = isUnlocked,
                            onPreview = { onPreviewEmoji(emoji) },
                            onBuy = { onBuyEmoji(emoji) }
                        )
                    }
                }
            }
            2 -> {
                // Profile Skins Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("profile_skins_grid"),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allProfileSkins, key = { it.id }) { profileSkin ->
                        val isUnlocked = profileSkin.isDefault || unlockedAvatarSkinIds.contains(profileSkin.id)
                        val isEquipped = userProfile.photoUrl == "skin:${profileSkin.id}" || (profileSkin.isDefault && (userProfile.photoUrl == null || userProfile.photoUrl == "skin:skin_default"))

                        ProfileSkinCard(
                            skin = profileSkin,
                            isUnlocked = isUnlocked,
                            isEquipped = isEquipped,
                            onPreview = { onPreviewProfileSkin(profileSkin) },
                            onBuy = { onBuyProfileSkin(profileSkin) },
                            onEquip = { onEquipProfileSkin(profileSkin) }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Ball Skin Card Component
// -------------------------------------------------------------
@Composable
fun BallSkinCard(
    skin: BallSkin,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    userLevel: Int = 1,
    onPreview: () -> Unit,
    onBuy: () -> Unit,
    onEquip: () -> Unit
) {
    val rarityColor = getRarityColor(skin.rarity)
    val numberFormatter = NumberFormat.getNumberInstance(Locale.US)
    val isLevelLocked = !isUnlocked && userLevel < skin.requiredLevel

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onPreview() }
            .testTag("ball_card_${skin.id}"),
        colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
        border = BorderStroke(
            1.5.dp,
            if (isEquipped) NeonCyan else if (isUnlocked) rarityColor.copy(alpha = 0.5f) else NeonBorder
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Row: Rarity Tag & Level / Equipped Check
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(rarityColor.copy(alpha = 0.2f))
                        .border(1.dp, rarityColor.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = skin.rarity.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = rarityColor
                    )
                }

                if (isEquipped) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .border(1.dp, NeonCyan, RoundedCornerShape(10.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EQUIPPED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonCyan
                        )
                    }
                } else if (isUnlocked) {
                    Text(
                        text = "OWNED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonEmerald
                    )
                } else if (isLevelLocked) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF3B1528))
                            .border(1.dp, NeonMagenta.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🔒 Lv.${skin.requiredLevel}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonMagenta
                        )
                    }
                } else {
                    Text(
                        text = "Lv.${skin.requiredLevel}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Ball Visual Orb
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F1424))
                    .border(1.5.dp, if (isLevelLocked) Color.Gray.copy(alpha = 0.4f) else rarityColor.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = skin.drawableResId),
                    contentDescription = skin.name,
                    modifier = Modifier
                        .size(58.dp)
                        .padding(2.dp)
                        .alpha(if (isLevelLocked) 0.65f else 1f),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Ball Name
            Text(
                text = skin.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Tag or Short Description
            Text(
                text = skin.tag,
                fontSize = 10.sp,
                color = Color(0xFFA0ACCC),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Button
            if (isEquipped) {
                Button(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = NeonCyan.copy(alpha = 0.25f),
                        disabledContentColor = NeonCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ACTIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else if (isUnlocked) {
                Button(
                    onClick = onEquip,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonDarkSurface,
                        contentColor = NeonCyan
                    ),
                    border = BorderStroke(1.dp, NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("equip_ball_${skin.id}")
                ) {
                    Text("EQUIP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else if (isLevelLocked) {
                Button(
                    onClick = onBuy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF261828),
                        contentColor = NeonMagenta
                    ),
                    border = BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("buy_ball_${skin.id}")
                ) {
                    Text(
                        text = "🔒 Lv.${skin.requiredLevel} · 🪙 ${numberFormatter.format(skin.priceCoins)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Button(
                    onClick = onBuy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonAmber,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("buy_ball_${skin.id}")
                ) {
                    Text(
                        text = "🪙 ${numberFormatter.format(skin.priceCoins)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Emoji Skin Card Component
// -------------------------------------------------------------
@Composable
fun EmojiSkinCard(
    emoji: EmojiSkin,
    isUnlocked: Boolean,
    onPreview: () -> Unit,
    onBuy: () -> Unit
) {
    val numberFormatter = NumberFormat.getNumberInstance(Locale.US)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onPreview() }
            .testTag("emoji_card_${emoji.id}"),
        colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
        border = BorderStroke(1.5.dp, if (isUnlocked) NeonEmerald.copy(alpha = 0.6f) else NeonBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isUnlocked) {
                    Text(
                        text = "UNLOCKED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonEmerald
                    )
                } else {
                    Text(
                        text = "LOCKED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8E9CBF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Emoji Symbol Box
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(NeonDarkSurface)
                    .border(1.dp, NeonBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji.symbol, fontSize = 34.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = emoji.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = emoji.tag,
                fontSize = 10.sp,
                color = Color(0xFFA0ACCC),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isUnlocked) {
                Button(
                    onClick = onPreview,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonDarkSurface,
                        contentColor = NeonEmerald
                    ),
                    border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.8f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PLAY SOUND", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onBuy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonAmber,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("buy_emoji_${emoji.id}")
                ) {
                    Text(
                        text = "🪙 ${numberFormatter.format(emoji.priceCoins)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Profile Skin Card Component
// -------------------------------------------------------------
@Composable
fun ProfileSkinCard(
    skin: ProfileSkin,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onPreview: () -> Unit,
    onBuy: () -> Unit,
    onEquip: () -> Unit
) {
    val numberFormatter = NumberFormat.getNumberInstance(Locale.US)
    val primaryColor = Color(skin.primaryColorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onPreview() }
            .testTag("profile_card_${skin.id}"),
        colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
        border = BorderStroke(
            1.5.dp,
            if (isEquipped) NeonCyan else if (isUnlocked) primaryColor.copy(alpha = 0.6f) else NeonBorder
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(primaryColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = skin.symbol,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryColor
                    )
                }

                if (isEquipped) {
                    Text(
                        text = "EQUIPPED",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan
                    )
                } else if (isUnlocked) {
                    Text(
                        text = "OWNED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Avatar Preview Badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(skin.secondaryColorHex))
                    .border(2.dp, primaryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = skin.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = skin.title,
                fontSize = 10.sp,
                color = Color(0xFFA0ACCC),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isEquipped) {
                Button(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = NeonCyan.copy(alpha = 0.25f),
                        disabledContentColor = NeonCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ACTIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else if (isUnlocked) {
                Button(
                    onClick = onEquip,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonDarkSurface,
                        contentColor = NeonCyan
                    ),
                    border = BorderStroke(1.dp, NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("equip_profile_${skin.id}")
                ) {
                    Text("EQUIP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onBuy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonAmber,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("buy_profile_${skin.id}")
                ) {
                    Text(
                        text = "🪙 ${numberFormatter.format(skin.priceCoins)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Detail Inspection Dialogs
// -------------------------------------------------------------
@Composable
fun BallSkinDetailDialog(
    skin: BallSkin,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    userCoins: Int,
    userLevel: Int = 1,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    onDismiss: () -> Unit
) {
    val rarityColor = getRarityColor(skin.rarity)
    val numberFormatter = NumberFormat.getNumberInstance(Locale.US)
    val isLevelLocked = !isUnlocked && userLevel < skin.requiredLevel
    val infiniteTransition = rememberInfiniteTransition(label = "BallGlow")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BallScale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeonDarkCard,
        shape = RoundedCornerShape(20.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Rarity & Level Tag Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(rarityColor.copy(alpha = 0.2f))
                            .border(1.dp, rarityColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${skin.rarity.uppercase()} · ${skin.tag}",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = rarityColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLevelLocked) Color(0xFF3B1528) else NeonCyan.copy(alpha = 0.2f))
                            .border(1.dp, if (isLevelLocked) NeonMagenta else NeonCyan, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isLevelLocked) "🔒 Requires Lv.${skin.requiredLevel}" else "⚡ Lv.${skin.requiredLevel}",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = if (isLevelLocked) NeonMagenta else NeonCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hero Ball Visual Preview
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scaleAnim)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    rarityColor.copy(alpha = 0.35f),
                                    Color(0xFF0C101F)
                                )
                            )
                        )
                        .border(2.5.dp, if (isLevelLocked) NeonMagenta.copy(alpha = 0.8f) else rarityColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = skin.drawableResId),
                        contentDescription = skin.name,
                        modifier = Modifier.size(90.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = skin.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = skin.description,
                    fontSize = 13.sp,
                    color = Color(0xFFA0ACCC),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (!isUnlocked) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonDarkSurface)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Price:", color = Color(0xFFA0ACCC), fontSize = 13.sp)
                        Text(
                            text = "🪙 ${numberFormatter.format(skin.priceCoins)} Coins",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = NeonAmber
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isEquipped) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Currently Equipped", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else if (isUnlocked) {
                Button(
                    onClick = {
                        onEquip()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Equip Ball Skin", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else if (isLevelLocked) {
                Button(
                    onClick = {
                        onBuy()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1528)),
                    border = BorderStroke(1.dp, NeonMagenta),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "🔒 Requires Level ${skin.requiredLevel} (🪙 ${numberFormatter.format(skin.priceCoins)})",
                        color = NeonMagenta,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            } else {
                Button(
                    onClick = {
                        onBuy()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Unlock for 🪙 ${numberFormatter.format(skin.priceCoins)}",
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, NeonBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", color = Color.White)
            }
        }
    )
}

@Composable
fun EmojiDetailDialog(
    emoji: EmojiSkin,
    isUnlocked: Boolean,
    userCoins: Int,
    onBuy: () -> Unit,
    onDismiss: () -> Unit
) {
    val numberFormatter = NumberFormat.getNumberInstance(Locale.US)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeonDarkCard,
        shape = RoundedCornerShape(20.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = emoji.symbol,
                    fontSize = 54.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = emoji.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = emoji.description,
                    fontSize = 13.sp,
                    color = Color(0xFFA0ACCC),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (!isUnlocked) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonDarkSurface)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Price:", color = Color(0xFFA0ACCC), fontSize = 13.sp)
                        Text(
                            text = "🪙 ${numberFormatter.format(emoji.priceCoins)} Coins",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = NeonAmber
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isUnlocked) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Owned & Ready", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        onBuy()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Unlock for 🪙 ${numberFormatter.format(emoji.priceCoins)}",
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, NeonBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", color = Color.White)
            }
        }
    )
}

@Composable
fun ProfileSkinDetailDialog(
    skin: ProfileSkin,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    userCoins: Int,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    onDismiss: () -> Unit
) {
    val primaryColor = Color(skin.primaryColorHex)
    val numberFormatter = NumberFormat.getNumberInstance(Locale.US)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeonDarkCard,
        shape = RoundedCornerShape(20.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(Color(skin.secondaryColorHex))
                        .border(3.dp, primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = skin.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                    color = Color.White
                )

                Text(
                    text = skin.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = skin.description,
                    fontSize = 13.sp,
                    color = Color(0xFFA0ACCC),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            if (isEquipped) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Currently Equipped", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else if (isUnlocked) {
                Button(
                    onClick = {
                        onEquip()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Equip Profile Suit", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        onBuy()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Unlock for 🪙 ${numberFormatter.format(skin.priceCoins)}",
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, NeonBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", color = Color.White)
            }
        }
    )
}

fun getRarityColor(rarity: String): Color {
    return when (rarity.lowercase()) {
        "starter" -> Color(0xFF00E5FF)
        "common" -> Color(0xFF00E676)
        "uncommon" -> Color(0xFF29B6F6)
        "rare" -> Color(0xFFAB47BC)
        "epic" -> Color(0xFFFF4081)
        "legendary" -> Color(0xFFFFD700)
        "mythic" -> Color(0xFFFF5252)
        "exalted" -> Color(0xFFE040FB)
        else -> Color(0xFF00E5FF)
    }
}
