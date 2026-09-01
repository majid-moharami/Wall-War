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
import com.wallwar.BuildConfig
import com.wallwar.data.billing.BillingConstants
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
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
import com.wallwar.data.billing.StoreBillingType
import com.wallwar.ui.theme.NeonAmber
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonDarkBg
import com.wallwar.ui.theme.NeonDarkCard
import com.wallwar.ui.theme.NeonDarkSurface
import com.wallwar.ui.theme.NeonEmerald
import com.wallwar.ui.theme.NeonMagenta
import kotlinx.coroutines.delay

@Composable
fun CoinShopScreen(
    userProfile: UserProfile,
    coinPacks: List<CoinPack>,
    purchaseMessage: String?,
    activeStore: StoreBillingType = StoreBillingType.GOOGLE_PLAY,
    isPurchasing: Boolean = false,
    isRewardedAdLoading: Boolean = false,
    isRewardedAdReady: Boolean = true,
    isAdPlaying: Boolean = false,
    onSelectStore: (StoreBillingType) -> Unit = {},
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
                    if (BuildConfig.DEBUG) {
                        Text(
                            text = "In-App Billing • ${activeStore.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }
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

        // Active Store Banner (Shown only in DEBUG mode)
        if (BuildConfig.DEBUG) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                border = BorderStroke(1.dp, when (activeStore) {
                    StoreBillingType.CAFE_BAZAAR -> NeonEmerald.copy(alpha = 0.5f)
                    StoreBillingType.MYKET -> NeonMagenta.copy(alpha = 0.5f)
                    StoreBillingType.GOOGLE_PLAY -> NeonCyan.copy(alpha = 0.5f)
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = when (activeStore) {
                                StoreBillingType.CAFE_BAZAAR -> NeonEmerald.copy(alpha = 0.2f)
                                StoreBillingType.MYKET -> NeonMagenta.copy(alpha = 0.2f)
                                StoreBillingType.GOOGLE_PLAY -> NeonCyan.copy(alpha = 0.2f)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = null,
                                    tint = when (activeStore) {
                                        StoreBillingType.CAFE_BAZAAR -> NeonEmerald
                                        StoreBillingType.MYKET -> NeonMagenta
                                        StoreBillingType.GOOGLE_PLAY -> NeonCyan
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Payment Method",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA0ACCC)
                            )
                            Text(
                                text = when (activeStore) {
                                    StoreBillingType.CAFE_BAZAAR -> "کافه بازار (Cafe Bazaar In-App Billing)"
                                    StoreBillingType.MYKET -> "مایکت (Myket In-App Billing)"
                                    StoreBillingType.GOOGLE_PLAY -> "Google Play Billing (Official)"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (activeStore) {
                            StoreBillingType.CAFE_BAZAAR -> NeonEmerald.copy(alpha = 0.15f)
                            StoreBillingType.MYKET -> NeonMagenta.copy(alpha = 0.15f)
                            StoreBillingType.GOOGLE_PLAY -> NeonCyan.copy(alpha = 0.15f)
                        },
                        border = BorderStroke(
                            1.dp,
                            when (activeStore) {
                                StoreBillingType.CAFE_BAZAAR -> NeonEmerald
                                StoreBillingType.MYKET -> NeonMagenta
                                StoreBillingType.GOOGLE_PLAY -> NeonCyan
                            }
                        )
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = when (activeStore) {
                                StoreBillingType.CAFE_BAZAAR -> NeonEmerald
                                StoreBillingType.MYKET -> NeonMagenta
                                StoreBillingType.GOOGLE_PLAY -> NeonCyan
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Purchase Success / Error Toast
        AnimatedVisibility(
            visible = purchaseMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (purchaseMessage?.startsWith("❌") == true || purchaseMessage?.startsWith("⚠️") == true)
                        Color(0xFF381515) else Color(0xFF0F3822)
                ),
                border = BorderStroke(
                    1.dp,
                    if (purchaseMessage?.startsWith("❌") == true || purchaseMessage?.startsWith("⚠️") == true)
                        NeonMagenta else Color(0xFF22C55E)
                ),
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
                        contentDescription = "Notification",
                        tint = if (purchaseMessage?.startsWith("❌") == true || purchaseMessage?.startsWith("⚠️") == true)
                            NeonMagenta else Color(0xFF22C55E),
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
                    if (isRewardedAdLoading || isAdPlaying) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAdPlaying) "Playing..." else "Loading...",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.OndemandVideo,
                            contentDescription = "Watch Ad",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Watch",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Section Title: Coin Bundles
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "COIN PACKAGES",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }

        // Coin Packs 2-Column Grid Layout
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            coinPacks.chunked(2).forEach { rowPacks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPacks.forEach { pack ->
                        Box(modifier = Modifier.weight(1f)) {
                            CoinPackGridCard(
                                pack = pack,
                                isPurchasing = isPurchasing,
                                activeStore = activeStore,
                                onBuy = { onBuyPack(pack) }
                            )
                        }
                    }
                    if (rowPacks.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security & In-App Purchase Footer
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = NeonDarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🔒 Secure In-App Billing",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Payments are processed securely via ${activeStore.displayName}. Coins are instantly credited to your wallet and synced with Nakama Online Servers.",
                    color = Color(0xFF8E95AA),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun CoinPackGridCard(
    pack: CoinPack,
    isPurchasing: Boolean,
    activeStore: StoreBillingType,
    onBuy: () -> Unit
) {
    val iconVector = when {
        pack.coins >= 5000 -> Icons.Default.WorkspacePremium
        pack.coins >= 2000 -> Icons.Default.EmojiEvents
        pack.coins >= 1000 -> Icons.Default.Savings
        pack.coins >= 500 -> Icons.Default.Star
        else -> Icons.Default.CardGiftcard
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
        border = BorderStroke(1.dp, Color(0xFF2E3554)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("coin_pack_${pack.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Unified Icon Badge (Same Gold/Amber aesthetic across all cards)
            Surface(
                shape = CircleShape,
                color = NeonAmber.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.35f)),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = pack.nameEn,
                        tint = NeonAmber,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pack Title and Enhanced Mini-Tag next to title with fixed single-line alignment
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
            ) {
                Text(
                    text = pack.nameEn,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                if (pack.popularTag != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    val tagColor = when (pack.popularTag) {
                        "BEST VALUE" -> Color(0xFFFFD700) // Gold
                        "GREAT VALUE" -> NeonMagenta // Magenta / Pink
                        "POPULAR" -> Color(0xFF00E5FF) // Electric Blue / Cyan
                        else -> NeonEmerald
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = tagColor.copy(alpha = 0.18f),
                        border = BorderStroke(0.75.dp, tagColor.copy(alpha = 0.85f))
                    ) {
                        Text(
                            text = pack.popularTag,
                            color = tagColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 6.5.sp,
                            lineHeight = 7.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Coins Value (Fixed height for alignment)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = NeonAmber,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "+${pack.coins} Coins",
                    color = NeonAmber,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Unified Buy Button (Same color across all packs)
            val displayPrice = if (activeStore == StoreBillingType.MYKET || activeStore == StoreBillingType.CAFE_BAZAAR) {
                BillingConstants.getTomanPriceForCoins(pack.coins)
            } else {
                pack.priceUsd
            }

            Button(
                onClick = onBuy,
                enabled = !isPurchasing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF333A4D),
                    disabledContentColor = Color(0xFF8893A8)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("buy_pack_${pack.id}_button")
            ) {
                if (isPurchasing) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = displayPrice,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
