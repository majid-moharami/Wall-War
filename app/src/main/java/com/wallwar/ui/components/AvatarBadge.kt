package com.wallwar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wallwar.data.ProfileSkin
import com.wallwar.data.ProfileSkinCatalog
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonDarkSurface

@Composable
fun AvatarBadge(
    photoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    borderWidth: Dp = 2.dp,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val skin: ProfileSkin? = if (ProfileSkinCatalog.isSkinUrl(photoUrl)) {
        ProfileSkinCatalog.getSkinById(photoUrl)
    } else {
        null
    }

    val isWebImage = !photoUrl.isNullOrBlank() && 
            (photoUrl.startsWith("http://") || photoUrl.startsWith("https://"))

    val effectiveBorderColor = borderColor ?: if (skin != null) {
        Color(skin.primaryColorHex)
    } else {
        NeonCyan
    }

    val baseModifier = modifier
        .size(size)
        .clip(CircleShape)
        .then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )

    if (isWebImage) {
        Box(
            modifier = baseModifier
                .background(NeonDarkSurface)
                .border(borderWidth, effectiveBorderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else if (skin != null) {
        val primaryCol = Color(skin.primaryColorHex)
        val secondaryCol = Color(skin.secondaryColorHex)
        Box(
            modifier = baseModifier
                .border(borderWidth, primaryCol, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CyberAvatarGraphic(
                skinId = skin.id,
                primaryColor = primaryCol,
                secondaryColor = secondaryCol,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // Classic Default Duelist Avatar
        Box(
            modifier = baseModifier
                .border(borderWidth, effectiveBorderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CyberAvatarGraphic(
                skinId = "skin_default",
                primaryColor = effectiveBorderColor,
                secondaryColor = Color(0xFF0F172A),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
