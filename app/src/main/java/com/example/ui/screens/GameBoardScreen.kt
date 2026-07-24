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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.engine.GameEngine
import com.example.model.BoardTheme
import com.example.model.GameState
import com.example.model.Position
import com.example.model.Wall
import com.example.ui.components.GameBoardComposable
import com.example.ui.theme.WallWarAmber
import com.example.ui.theme.WallWarPurple
import kotlin.math.roundToInt

@Composable
fun GameBoardScreen(
    gameState: GameState,
    boardTheme: BoardTheme,
    isWallMode: Boolean,
    isWallHorizontal: Boolean,
    validHighlights: List<Position>,
    soundManager: SoundManager,
    onCellClick: (r: Int, c: Int) -> Unit,
    onPlaceWall: (r: Int, c: Int, isHorizontal: Boolean) -> Unit,
    onSelectWallOrientation: (isHorizontal: Boolean) -> Unit,
    onUndoMove: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeDragWall by remember { mutableStateOf<Wall?>(null) }
    var isValidDrag by remember { mutableStateOf(false) }
    var boardBoundsInWindow by remember { mutableStateOf<Rect?>(null) }

    val handleStartWallDrag: (isHorizontal: Boolean, windowPos: Offset) -> Unit = { isHorizontal, windowPos ->
        onSelectWallOrientation(isHorizontal)
        val bounds = boardBoundsInWindow
        if (bounds != null) {
            val boardX = windowPos.x - bounds.left
            val boardY = windowPos.y - bounds.top

            val cols = gameState.cols
            val rows = gameState.rows
            val width = bounds.width
            val height = bounds.height
            val gapRatio = 0.18f
            val cellW = width / (cols + (cols - 1) * gapRatio)
            val cellH = height / (rows + (rows - 1) * gapRatio)
            val stepX = cellW + (cellW * gapRatio)
            val stepY = cellH + (cellH * gapRatio)

            val fingerOffsetUp = stepY * 2.5f
            val targetC = ((boardX / stepX) - 0.5f).roundToInt().coerceIn(0, cols - 2)
            val targetR = (((boardY - fingerOffsetUp) / stepY) - 0.5f).roundToInt().coerceIn(0, rows - 2)

            val candidate = Wall(targetR, targetC, isHorizontal, gameState.turn)
            activeDragWall = candidate
            isValidDrag = GameEngine.canPlaceWall(gameState, gameState.turn, candidate)
            soundManager.vibrateShort()
        }
    }

    val handleUpdateWallDrag: (isHorizontal: Boolean, windowPos: Offset) -> Unit = { isHorizontal, windowPos ->
        val bounds = boardBoundsInWindow
        if (bounds != null) {
            val boardX = windowPos.x - bounds.left
            val boardY = windowPos.y - bounds.top

            val cols = gameState.cols
            val rows = gameState.rows
            val width = bounds.width
            val height = bounds.height
            val gapRatio = 0.18f
            val cellW = width / (cols + (cols - 1) * gapRatio)
            val cellH = height / (rows + (rows - 1) * gapRatio)
            val stepX = cellW + (cellW * gapRatio)
            val stepY = cellH + (cellH * gapRatio)

            val fingerOffsetUp = stepY * 2.5f
            val targetC = ((boardX / stepX) - 0.5f).roundToInt().coerceIn(0, cols - 2)
            val targetR = (((boardY - fingerOffsetUp) / stepY) - 0.5f).roundToInt().coerceIn(0, rows - 2)

            val current = activeDragWall
            if (current == null || current.r != targetR || current.c != targetC || current.isHorizontal != isHorizontal) {
                val candidate = Wall(targetR, targetC, isHorizontal, gameState.turn)
                activeDragWall = candidate
                isValidDrag = GameEngine.canPlaceWall(gameState, gameState.turn, candidate)
                soundManager.vibrateShort()
            }
        }
    }

    val handleEndWallDrag: () -> Unit = {
        val wall = activeDragWall
        val valid = isValidDrag
        if (wall != null) {
            if (valid) {
                onPlaceWall(wall.r, wall.c, wall.isHorizontal)
            } else {
                soundManager.playErrorSound()
            }
        }
        activeDragWall = null
        isValidDrag = false
    }
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

        // Top Players Score Header Bar (P1 & P2 / AI)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlayerScoreCard(
                playerName = "Player 1",
                wallsLeft = gameState.leftWalls[0],
                isTurn = gameState.turn == 0 && gameState.winner == null,
                pawnColor = WallWarPurple,
                modifier = Modifier.weight(1f)
            )

            PlayerScoreCard(
                playerName = if (gameState.isAiMatch) "AI Bot" else "Player 2",
                wallsLeft = gameState.leftWalls[1],
                isTurn = gameState.turn == 1 && gameState.winner == null,
                pawnColor = WallWarAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Center Game Board View
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    boardBoundsInWindow = coordinates.boundsInWindow()
                },
            contentAlignment = Alignment.Center
        ) {
            GameBoardComposable(
                gameState = gameState,
                boardTheme = boardTheme,
                isWallMode = isWallMode,
                isWallHorizontal = isWallHorizontal,
                validHighlights = validHighlights,
                soundManager = soundManager,
                onCellClick = onCellClick,
                onPlaceWall = onPlaceWall,
                externalDragWall = activeDragWall,
                externalIsValidDrag = isValidDrag
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Single Active Player Wall Controls Panel (Updates dynamically on turn change)
        val activeTurn = gameState.turn
        val activePlayerName = if (activeTurn == 0) "Player 1" else if (gameState.isAiMatch) "AI Bot" else "Player 2"
        val activePawnColor = if (activeTurn == 0) WallWarPurple else WallWarAmber

        PlayerWallControlRow(
            playerName = activePlayerName,
            pawnColor = activePawnColor,
            isTurn = gameState.winner == null,
            wallsLeft = gameState.leftWalls[activeTurn],
            isWallMode = isWallMode,
            isWallHorizontal = isWallHorizontal,
            onSelectWallOrientation = onSelectWallOrientation,
            onStartWallDrag = handleStartWallDrag,
            onUpdateWallDrag = handleUpdateWallDrag,
            onEndWallDrag = handleEndWallDrag
        )

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
                    colors = ButtonDefaults.buttonColors(containerColor = WallWarPurple)
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
    onSelectWallOrientation: (isHorizontal: Boolean) -> Unit,
    onStartWallDrag: (isHorizontal: Boolean, windowPos: Offset) -> Unit,
    onUpdateWallDrag: (isHorizontal: Boolean, windowPos: Offset) -> Unit,
    onEndWallDrag: () -> Unit,
    modifier: Modifier = Modifier
) {
    var horizBounds by remember { mutableStateOf<Rect?>(null) }
    var vertBounds by remember { mutableStateOf<Rect?>(null) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isTurn) pawnColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isTurn) androidx.compose.foundation.BorderStroke(2.dp, pawnColor) else null,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Player Avatar & Stock Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(pawnColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = playerName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (isTurn) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(pawnColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "TURN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (pawnColor == WallWarAmber) Color.Black else Color.White
                                )
                            }
                        }
                    }
                    Text(
                        text = "Walls: $wallsLeft",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Horizontal Wall Item Chip
            Button(
                onClick = { onSelectWallOrientation(true) },
                enabled = isTurn && wallsLeft > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isWallMode && isWallHorizontal && isTurn) WallWarAmber else pawnColor.copy(alpha = 0.25f),
                    contentColor = if (isWallMode && isWallHorizontal && isTurn) Color(0xFF381E72) else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(42.dp)
                    .onGloballyPositioned { horizBounds = it.boundsInWindow() }
                    .pointerInput(isTurn, wallsLeft) {
                        if (!isTurn || wallsLeft <= 0) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val bounds = horizBounds ?: return@awaitEachGesture
                            val windowPos = bounds.topLeft + down.position
                            onStartWallDrag(true, windowPos)

                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.pressed) {
                                    val currentPos = bounds.topLeft + change.position
                                    onUpdateWallDrag(true, currentPos)
                                    change.consume()
                                } else {
                                    onEndWallDrag()
                                    break
                                }
                            } while (true)
                        }
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.CropLandscape,
                    contentDescription = "Horizontal Wall Item",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "── Horiz",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Vertical Wall Item Chip
            Button(
                onClick = { onSelectWallOrientation(false) },
                enabled = isTurn && wallsLeft > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isWallMode && !isWallHorizontal && isTurn) WallWarAmber else pawnColor.copy(alpha = 0.25f),
                    contentColor = if (isWallMode && !isWallHorizontal && isTurn) Color(0xFF381E72) else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(42.dp)
                    .onGloballyPositioned { vertBounds = it.boundsInWindow() }
                    .pointerInput(isTurn, wallsLeft) {
                        if (!isTurn || wallsLeft <= 0) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val bounds = vertBounds ?: return@awaitEachGesture
                            val windowPos = bounds.topLeft + down.position
                            onStartWallDrag(false, windowPos)

                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.pressed) {
                                    val currentPos = bounds.topLeft + change.position
                                    onUpdateWallDrag(false, currentPos)
                                    change.consume()
                                } else {
                                    onEndWallDrag()
                                    break
                                }
                            } while (true)
                        }
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.CropPortrait,
                    contentDescription = "Vertical Wall Item",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "│ Vert",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
