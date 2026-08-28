package com.wallwar.ui.screens.shop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wallwar.data.UserProfile
import com.wallwar.ui.theme.NeonAmber
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonDarkBg
import com.wallwar.ui.theme.NeonDarkCard
import com.wallwar.ui.theme.NeonDarkSurface
import com.wallwar.ui.theme.NeonMagenta
import kotlinx.coroutines.delay

@Composable
fun CoinShopScreen(
    userProfile: UserProfile,
    coinPacks: List<CoinPack>,
    purchaseMessage: String?,
    isPurchasing: Boolean = false,
    isRewardedAdLoading: Boolean = false,
    isRewardedAdReady: Boolean = true,
    isAdPlaying: Boolean = false,
    onWatchRewardedAd: () -> Unit = {},
    onBuyPack: (CoinPack) -> Unit,
    onClearMessage: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(purchaseMessage) {
        if (purchaseMessage != null) {
            delay(3500)
            onClearMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonDarkBg)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("coin_shop_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Coin Store",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "In-App Store",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA0ACCC)
                    )
                }
            }

            // Current Coins Balance Badge
            Surface(
                shape = CircleShape,
                color = NeonDarkSurface,
                border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = NeonAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${userProfile.coins} Coins",
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonAmber,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Purchase Success Toast
        AnimatedVisibility(
            visible = purchaseMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3822)),
                border = BorderStroke(1.dp, Color(0xFF22C55E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = purchaseMessage ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 📺 Rewarded Ad Placement (+50 Free Coins)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(NeonCyan, NeonAmber))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, NeonCyan)
                        ) {
                            Text(
                                text = "FREE REWARD",
                                color = NeonCyan,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Watch Ad (+50 Coins)",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Watch a quick sponsored video to earn +50 Free Coins instantly!",
                        color = Color(0xFFA0ACCC),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onWatchRewardedAd,
                    enabled = !isRewardedAdLoading && !isAdPlaying,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonAmber,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF333A4D),
                        disabledContentColor = Color(0xFF8893A8)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("watch_rewarded_ad_button")
                ) {
                    if (isRewardedAdLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Loading Ad...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    } else if (isAdPlaying) {
                        Text(
                            text = "⏳ Showing Ad...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.OndemandVideo,
                            contentDescription = "Watch Ad",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+50 Coins",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Packages Table Card matching user provided list layout
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkSurface),
            border = BorderStroke(1.dp, Color(0xFF22293E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161A2B))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right: Package Name
                    Text(
                        text = "Package Name",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBAC5E1),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1.2f)
                    )

                    // Center: Coins Content
                    Text(
                        text = "Coins",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBAC5E1),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.1f)
                    )

                    // Left: Price
                    Text(
                        text = "Price",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBAC5E1),
                        fontSize = 13.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = Color(0xFF22293E), thickness = 1.dp)

                // Package Rows
                coinPacks.forEachIndexed { index, pack ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBuyPack(pack) }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: Package Name & Visual Icon
                            Row(
                                modifier = Modifier.weight(1.3f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Visual Coin Pack Photo / Badge
                                CoinPackVisualBadge(packId = pack.id, packTag = pack.popularTag)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = pack.nameEn,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                        if (pack.popularTag != null) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Popular",
                                                tint = NeonAmber,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    if (pack.popularTag != null) {
                                        Text(
                                            text = pack.popularTag,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonAmber
                                        )
                                    }
                                }
                            }

                            // Column 2: Content (Coins)
                            Row(
                                modifier = Modifier.weight(1.1f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatCoins(pack.coins),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonAmber,
                                    fontSize = 14.sp
                                )
                            }

                            // Column 3: Price Button - e.g. "$0.99"
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                CoinPriceButton(
                                    priceText = pack.priceUsd,
                                    onClick = { if (!isPurchasing) onBuyPack(pack) },
                                    enabled = !isPurchasing,
                                    testTag = "buy_pack_${pack.id}"
                                )
                            }
                        }

                        if (index < coinPacks.size - 1) {
                            HorizontalDivider(color = Color(0xFF1D2336), thickness = 1.dp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Security / Store Footer Notice
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF131726),
            border = BorderStroke(1.dp, Color(0xFF22293E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔒 Google Play In-App Billing & Nakama Server Sync",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA0ACCC),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Consumable coin packages • Instant delivery & server wallet verification",
                    color = Color(0xFF6B7A99),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CoinPriceButton(
    priceText: String,
    onClick: () -> Unit,
    enabled: Boolean,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor = when {
        !enabled -> Color(0xFF1A2332)
        isPressed -> Color(0xFF00A854)
        else -> Color(0xFF00E676)
    }

    val contentColor = when {
        !enabled -> Color(0xFF63728F)
        isPressed -> Color.White
        else -> Color(0xFF031A0D)
    }

    val borderColor = when {
        !enabled -> Color(0xFF263248)
        isPressed -> Color(0xFF00E676)
        else -> Color(0xFF69F0AE)
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color(0xFF1A2332),
            disabledContentColor = Color(0xFF63728F)
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
        modifier = modifier
            .defaultMinSize(minWidth = 78.dp, minHeight = 36.dp)
            .testTag(testTag)
    ) {
        Text(
            text = priceText,
            fontWeight = FontWeight.Black,
            color = contentColor,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

// Helper function to format coins count
private fun formatCoins(coins: Int): String {
    val formattedNumber = when (coins) {
        100 -> "100"
        300 -> "300"
        600 -> "600"
        1300 -> "1,300"
        3000 -> "3,000"
        7500 -> "7,500"
        else -> String.format("%,d", coins)
    }
    return "$formattedNumber Coins"
}

@Composable
private fun CoinPackVisualBadge(packId: String, packTag: String?) {
    val (bgColor, iconVector, iconTint) = when (packId) {
        "micro" -> Triple(Color(0xFF2B200A), Icons.Default.MonetizationOn, NeonAmber)
        "starter" -> Triple(Color(0xFF132838), Icons.Default.Savings, NeonCyan)
        "gamer" -> Triple(Color(0xFF1A2617), Icons.Default.CardGiftcard, Color(0xFF22C55E))
        "pro" -> Triple(Color(0xFF331631), Icons.Default.WorkspacePremium, NeonMagenta)
        "master" -> Triple(Color(0xFF3D2708), Icons.Default.EmojiEvents, NeonAmber)
        "champion" -> Triple(Color(0xFF3B181A), Icons.Default.Star, Color(0xFFFF4757))
        else -> Triple(NeonDarkSurface, Icons.Default.MonetizationOn, NeonAmber)
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                1.dp,
                if (packTag != null) iconTint else iconTint.copy(alpha = 0.4f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = "Pack Icon",
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
    }
}

