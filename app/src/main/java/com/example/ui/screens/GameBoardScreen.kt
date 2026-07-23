package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.model.BoardTheme
import com.example.model.GameState
import com.example.model.Position
import com.example.ui.components.GameBoardComposable
import com.example.ui.theme.WallRushAmber
import com.example.ui.theme.WallRushPurple

@Composable
fun GameBoardScreen(
    gameState: GameState,
    boardTheme: BoardTheme,
    isWallMode: Boolean,
    isWallHorizontal: Boolean,
    validHighlights: List<Position>,
    soundManager: SoundManager,
    onCellClick: (r: Int, c: Int) -> Unit,
    onToggleWallMode: () -> Unit,
    onToggleWallOrientation: () -> Unit,
    onUndoMove: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btn_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = gameState.mode.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (gameState.isAiMatch) "VS AI (${gameState.aiDifficulty.displayName})" else "Pass & Play",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    soundManager.isSoundEnabled = !soundManager.isSoundEnabled
                },
                modifier = Modifier.testTag("btn_sound_toggle")
            ) {
                Icon(
                    imageVector = if (soundManager.isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Sound Toggle"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Top Player Section
        if (!gameState.isAiMatch) {
            // Flipped 180° for Player 2 sitting across the table
            PlayerWallControlRow(
                playerName = "Player 2",
                pawnColor = WallRushAmber,
                isTurn = gameState.turn == 1 && gameState.winner == null,
                wallsLeft = gameState.leftWalls[1],
                isWallMode = isWallMode,
                isWallHorizontal = isWallHorizontal,
                onToggleWallMode = onToggleWallMode,
                onToggleWallOrientation = onToggleWallOrientation,
                modifier = Modifier.graphicsLayer { rotationZ = 180f }
            )
        } else {
            // Players Status Cards Header for VS AI Match
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlayerScoreCard(
                    playerName = "Player 1",
                    wallsLeft = gameState.leftWalls[0],
                    isTurn = gameState.turn == 0 && gameState.winner == null,
                    pawnColor = WallRushPurple,
                    modifier = Modifier.weight(1f)
                )

                PlayerScoreCard(
                    playerName = "AI Bot",
                    wallsLeft = gameState.leftWalls[1],
                    isTurn = gameState.turn == 1 && gameState.winner == null,
                    pawnColor = WallRushAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Center Game Board View
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            GameBoardComposable(
                gameState = gameState,
                boardTheme = boardTheme,
                isWallMode = isWallMode,
                isWallHorizontal = isWallHorizontal,
                validHighlights = validHighlights,
                onCellClick = onCellClick
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Player Section
        if (!gameState.isAiMatch) {
            // Normal 0° for Player 1 sitting at bottom
            PlayerWallControlRow(
                playerName = "Player 1",
                pawnColor = WallRushPurple,
                isTurn = gameState.turn == 0 && gameState.winner == null,
                wallsLeft = gameState.leftWalls[0],
                isWallMode = isWallMode,
                isWallHorizontal = isWallHorizontal,
                onToggleWallMode = onToggleWallMode,
                onToggleWallOrientation = onToggleWallOrientation
            )
        } else {
            // Wall Controls Bar for VS AI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onToggleWallMode,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isWallMode) WallRushAmber else WallRushPurple
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("btn_wall_mode")
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isWallMode) "Step Pawn" else "Place Wall",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onToggleWallOrientation,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("btn_wall_orientation")
                ) {
                    Icon(
                        imageVector = if (isWallHorizontal) Icons.Default.CropLandscape else Icons.Default.CropPortrait,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isWallHorizontal) "Horizontal ──" else "Vertical │",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary Action Controls (Undo & Restart)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = onUndoMove,
                enabled = gameState.moveHistory.isNotEmpty() && gameState.winner == null,
                modifier = Modifier.testTag("btn_undo")
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Undo"
                )
            }

            IconButton(
                onClick = onRestart,
                modifier = Modifier.testTag("btn_restart")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Restart"
                )
            }
        }
    }

    // Victory Celebration Dialog Modal
    if (gameState.winner != null) {
        val winnerName = if (gameState.winner == 0) "Player 1" else if (gameState.isAiMatch) "AI Bot" else "Player 2"

        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "🏆 $winnerName Wins!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Congratulations! You mastered the walls.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Total Moves: ${gameState.moveHistory.size}")
                    Text("Walls Placed: ${gameState.walls.size}")
                }
            },
            confirmButton = {
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = WallRushPurple)
                ) {
                    Text("Play Again")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onBack) {
                    Text("Main Menu")
                }
            }
        )
    }
}

@Composable
fun PlayerScoreCard(
    playerName: String,
    wallsLeft: Int,
    isTurn: Boolean,
    pawnColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isTurn) pawnColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(pawnColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Walls: $wallsLeft",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isTurn) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(pawnColor)
                )
            }
        }
    }
}

@Composable
fun PlayerWallControlRow(
    playerName: String,
    pawnColor: Color,
    isTurn: Boolean,
    wallsLeft: Int,
    isWallMode: Boolean,
    isWallHorizontal: Boolean,
    onToggleWallMode: () -> Unit,
    onToggleWallOrientation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isTurn) pawnColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(pawnColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = playerName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Walls: $wallsLeft",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onToggleWallMode,
                enabled = isTurn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isWallMode && isTurn) WallRushAmber else pawnColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isWallMode && isTurn) "Step Pawn" else "Place Wall",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onToggleWallOrientation,
                enabled = isTurn,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(
                    imageVector = if (isWallHorizontal) Icons.Default.CropLandscape else Icons.Default.CropPortrait,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isWallHorizontal) "Horiz ──" else "Vert │",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
