package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.WallWarAmber
import com.example.ui.theme.WallWarPurple

@Composable
fun RulesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btn_rules_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "How to Play WallWar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        RuleCard(
            title = "1. The Objective",
            description = "Be the first player to reach any cell on your target goal row across the board.",
            icon = Icons.Default.Navigation,
            iconTint = WallWarPurple
        )

        Spacer(modifier = Modifier.height(14.dp))

        RuleCard(
            title = "2. Turn Actions",
            description = "On your turn, you can either move your pawn 1 square orthogonally OR place 1 wall from your reserve.",
            icon = Icons.Default.GridOn,
            iconTint = WallWarAmber
        )

        Spacer(modifier = Modifier.height(14.dp))

        RuleCard(
            title = "3. Jumping over Opponents",
            description = "When adjacent to your opponent, you can jump directly over them! If a wall is behind them, you can jump diagonally.",
            icon = Icons.Default.SwapCalls,
            iconTint = WallWarPurple
        )

        Spacer(modifier = Modifier.height(14.dp))

        RuleCard(
            title = "4. The Golden Rule (No Trapping)",
            description = "Walls span 2 spaces. You are STRICTLY FORBIDDEN from completely trapping any player. Every player must always have at least ONE clear path to their goal!",
            icon = Icons.Default.Lock,
            iconTint = WallWarAmber
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RuleCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
