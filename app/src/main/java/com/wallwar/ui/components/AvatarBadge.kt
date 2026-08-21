package com.wallwar.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
    val isWebImage = !photoUrl.isNullOrBlank() && 
            (photoUrl.startsWith("http://") || photoUrl.startsWith("https://") || photoUrl.startsWith("content://") || photoUrl.startsWith("file://"))

    val effectiveBorderColor = borderColor ?: NeonCyan

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
                contentDescription = "Profile Picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // Fallback simple user icon for profile
        Box(
            modifier = baseModifier
                .background(NeonDarkSurface)
                .border(borderWidth, effectiveBorderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile Icon",
                tint = Color(0xFFA0ACCC),
                modifier = Modifier.size(size * 0.58f)
            )
        }
    }
}
