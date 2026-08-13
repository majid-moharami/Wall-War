package com.wallwar.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wallwar.audio.SoundManager
import com.wallwar.engine.GameEngine
import com.wallwar.model.BoardTheme
import com.wallwar.model.GameMode
import com.wallwar.model.GameState
import com.wallwar.model.Position
import com.wallwar.model.Wall
import com.wallwar.ui.components.GameBoardComposable
import com.wallwar.ui.theme.NeonBorder
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonDarkBg
import com.wallwar.ui.theme.NeonDarkCard
import com.wallwar.ui.theme.NeonDarkSurface
import com.wallwar.ui.theme.NeonMagenta
import kotlin.math.hypot
import kotlin.math.roundToInt

import androidx.compose.material3.CircularProgressIndicator
import com.wallwar.data.nakama.OnlineMatchState
import com.wallwar.model.OpponentType

@Composable
fun GameBoardScreen(
    gameState: GameState,
    boardTheme: BoardTheme,
    isWallMode: Boolean,
    isWallHorizontal: Boolean,
    validHighlights: List<Position>,
    soundManager: SoundManager,
    userDisplayName: String = "You",
    opponentType: OpponentType = OpponentType.AI,
    onlineMatchState: OnlineMatchState = OnlineMatchState.IDLE,
    onlineOpponentName: String = "Online Opponent",
    myPlayerIndex: Int = 0,
    turnTimeLeft: Int = 30,
    arenaTitle: String = "Pro Arena",
    onlineErrorMessage: String? = null,
    onRetryOnlineConnection: () -> Unit = {},
    onCancelOnlineMatchmaking: () -> Unit = {},
    onCellClick: (r: Int, c: Int) -> Unit,
    onPlaceWall: (r: Int, c: Int, isHorizontal: Boolean) -> Unit,
    onSelectWallOrientation: (isHorizontal: Boolean) -> Unit,
    onCancelWallMode: () -> Unit,
    onUndoMove: () -> Unit,
    onRestart: () -> Unit,
    onResign: () -> Unit = {},
    onBack: () -> Unit,
    onTriggerMatchEndInterstitial: (onClosed: () -> Unit) -> Unit = { it() },
    modifier: Modifier = Modifier
) {
    var activeDragWall by remember { mutableStateOf<Wall?>(null) }
    var isValidDrag by remember { mutableStateOf(false) }
    var boardBoundsInWindow by remember { mutableStateOf<Rect?>(null) }
    var showResignConfirmation by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }

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
            val isPlayer2Local = (opponentType == OpponentType.LOCAL_PASS_PLAY && gameState.mode == GameMode.DUEL && gameState.turn == 1)
            val targetY = if (isPlayer2Local) (boardY + fingerOffsetUp) else (boardY - fingerOffsetUp)

            val rawC = ((boardX / stepX) - 0.5f).roundToInt()
            val rawR = ((targetY / stepY) - 0.5f).roundToInt()

            val shouldFlip = (opponentType == OpponentType.ONLINE && myPlayerIndex == 1)
            val logicR = if (shouldFlip) (rows - 2 - rawR) else rawR
            val logicC = if (shouldFlip) (cols - 2 - rawC) else rawC

            val marginX = stepX * 0.8f
            val marginY = stepY * 0.8f

            val isOutside = boardX < -marginX || boardX > width + marginX ||
                    targetY < -marginY || targetY > height + marginY ||
                    logicC !in 0..(cols - 2) || logicR !in 0..(rows - 2)

            if (isOutside) {
                activeDragWall = null
                isValidDrag = false
            } else {
                val candidate = Wall(logicR, logicC, isHorizontal, gameState.turn)
                activeDragWall = candidate
                isValidDrag = GameEngine.canPlaceWall(gameState, gameState.turn, candidate)
                soundManager.vibrateShort()
            }
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
            val isPlayer2Local = (opponentType == OpponentType.LOCAL_PASS_PLAY && gameState.mode == GameMode.DUEL && gameState.turn == 1)
            val targetY = if (isPlayer2Local) (boardY + fingerOffsetUp) else (boardY - fingerOffsetUp)

            val rawC = ((boardX / stepX) - 0.5f).roundToInt()
            val rawR = ((targetY / stepY) - 0.5f).roundToInt()

            val shouldFlip = (opponentType == OpponentType.ONLINE && myPlayerIndex == 1)
            val logicR = if (shouldFlip) (rows - 2 - rawR) else rawR
            val logicC = if (shouldFlip) (cols - 2 - rawC) else rawC

            val marginX = stepX * 0.8f
            val marginY = stepY * 0.8f

            val isOutside = boardX < -marginX || boardX > width + marginX ||
                    targetY < -marginY || targetY > height + marginY ||
                    logicC !in 0..(cols - 2) || logicR !in 0..(rows - 2)

            if (isOutside) {
                if (activeDragWall != null) {
                    activeDragWall = null
                    isValidDrag = false
                }
            } else {
                val current = activeDragWall
                if (current == null || current.r != logicR || current.c != logicC || current.isHorizontal != isHorizontal) {
                    val candidate = Wall(logicR, logicC, isHorizontal, gameState.turn)
                    activeDragWall = candidate
                    isValidDrag = GameEngine.canPlaceWall(gameState, gameState.turn, candidate)
                    soundManager.vibrateShort()
                }
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
                onCancelWallMode()
            }
        } else {
            onCancelWallMode()
        }
        activeDragWall = null
        isValidDrag = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeonDarkBg)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Bar
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left: Back Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NeonDarkCard)
                    .border(1.dp, NeonBorder, CircleShape)
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        if (gameState.winner == null && (opponentType == OpponentType.ONLINE || gameState.isAiMatch)) {
                            showExitConfirmation = true
                        } else {
                            onBack()
                        }
                    },
                    modifier = Modifier.testTag("btn_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            // Center: Title & Subtitle
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = arenaTitle.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (opponentType == OpponentType.ONLINE) "ONLINE MULTIPLAYER" else if (gameState.isAiMatch) "VS AI (${gameState.aiDifficulty.displayName})" else "Pass & Play",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            // Right: Timer + Sound Toggle Button
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (gameState.winner == null && opponentType == OpponentType.ONLINE) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (turnTimeLeft < 10) NeonMagenta else NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${turnTimeLeft}s",
                            color = if (turnTimeLeft < 10) NeonMagenta else NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NeonDarkCard)
                        .border(1.dp, NeonBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            soundManager.isSoundEnabled = !soundManager.isSoundEnabled
                        },
                        modifier = Modifier.testTag("btn_sound_toggle")
                    ) {
                        Icon(
                            imageVector = if (soundManager.isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Sound Toggle",
                            tint = if (soundManager.isSoundEnabled) NeonCyan else Color(0xFFA0ACCC)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Top Section: Competitor Score Card / Player 2 Section
        val opponentName = if (opponentType == OpponentType.ONLINE) onlineOpponentName else if (gameState.isAiMatch) "AI Bot" else "Player 2"
        val opponentIndex = if (opponentType == OpponentType.ONLINE) (1 - myPlayerIndex) else 1
        val opponentPawnColor = if (opponentIndex == 0) NeonCyan else NeonMagenta
        val isQuickPassPlay = (opponentType == OpponentType.LOCAL_PASS_PLAY && gameState.mode == GameMode.DUEL)

        if (isQuickPassPlay) {
            // Player 2 Section (Rotated 180° for Player 2 standing/sitting on the opposite side)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { rotationZ = 180f },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Player 2 Wall Items (First in rotated column -> appears closer to board)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WallItemButton(
                        isHorizontal = true,
                        isSelected = isWallMode && gameState.turn == 1 && isWallHorizontal,
                        isEnabled = gameState.turn == 1 && gameState.leftWalls[1] > 0 && gameState.winner == null,
                        selectedColor = NeonMagenta,
                        isRotated = true,
                        onSelect = { onSelectWallOrientation(true) },
                        onStartDrag = { pos -> handleStartWallDrag(true, pos) },
                        onUpdateDrag = { pos -> handleUpdateWallDrag(true, pos) },
                        onEndDrag = handleEndWallDrag
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    WallItemButton(
                        isHorizontal = false,
                        isSelected = isWallMode && gameState.turn == 1 && !isWallHorizontal,
                        isEnabled = gameState.turn == 1 && gameState.leftWalls[1] > 0 && gameState.winner == null,
                        selectedColor = NeonMagenta,
                        isRotated = true,
                        onSelect = { onSelectWallOrientation(false) },
                        onStartDrag = { pos -> handleStartWallDrag(false, pos) },
                        onUpdateDrag = { pos -> handleUpdateWallDrag(false, pos) },
                        onEndDrag = handleEndWallDrag
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Player 2 Score Card (Second in rotated column -> appears at top edge of screen, above wall items from Player 2 point of view)
                PlayerScoreCard(
                    playerName = "Player 2",
                    wallsLeft = gameState.leftWalls[1],
                    isTurn = gameState.turn == 1 && gameState.winner == null,
                    pawnColor = NeonMagenta,
                    isAi = false,
                    modifier = Modifier.fillMaxWidth(0.7f)
                )
            }
        } else {
            PlayerScoreCard(
                playerName = opponentName,
                wallsLeft = gameState.leftWalls[opponentIndex],
                isTurn = gameState.turn == opponentIndex && gameState.winner == null,
                pawnColor = opponentPawnColor,
                isAi = gameState.isAiMatch && opponentIndex == 1,
                modifier = Modifier.fillMaxWidth(0.7f)
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
                onCancelWallMode = onCancelWallMode,
                shouldFlip = (opponentType == OpponentType.ONLINE && myPlayerIndex == 1),
                externalDragWall = activeDragWall,
                externalIsValidDrag = isValidDrag
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Section
        val activeTurn = gameState.turn
        val isLocalTurn = if (opponentType == OpponentType.ONLINE) {
            activeTurn == myPlayerIndex
        } else {
            !(gameState.isAiMatch && activeTurn == 1)
        }

        val myPawnColor = if (myPlayerIndex == 0) NeonCyan else NeonMagenta
        val activePlayerColor = if (gameState.turn == 0) NeonCyan else NeonMagenta
        val currentTurnColor = if (opponentType == OpponentType.LOCAL_PASS_PLAY) activePlayerColor else myPawnColor

        val p1WallEnabled = if (isQuickPassPlay) {
            gameState.turn == 0 && gameState.leftWalls[0] > 0 && gameState.winner == null
        } else {
            isLocalTurn && gameState.leftWalls[activeTurn] > 0 && gameState.winner == null
        }

        val p1WallSelected = if (isQuickPassPlay) {
            isWallMode && gameState.turn == 0
        } else {
            isWallMode
        }

        // Row 1: Wall items (Icons only)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WallItemButton(
                isHorizontal = true,
                isSelected = p1WallSelected && isWallHorizontal,
                isEnabled = p1WallEnabled,
                selectedColor = if (isQuickPassPlay) NeonCyan else currentTurnColor,
                onSelect = { onSelectWallOrientation(true) },
                onStartDrag = { pos -> handleStartWallDrag(true, pos) },
                onUpdateDrag = { pos -> handleUpdateWallDrag(true, pos) },
                onEndDrag = handleEndWallDrag
            )
            Spacer(modifier = Modifier.width(20.dp))
            WallItemButton(
                isHorizontal = false,
                isSelected = p1WallSelected && !isWallHorizontal,
                isEnabled = p1WallEnabled,
                selectedColor = if (isQuickPassPlay) NeonCyan else currentTurnColor,
                onSelect = { onSelectWallOrientation(false) },
                onStartDrag = { pos -> handleStartWallDrag(false, pos) },
                onUpdateDrag = { pos -> handleUpdateWallDrag(false, pos) },
                onEndDrag = handleEndWallDrag
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Local Player Info Card
        PlayerScoreCard(
            playerName = if (opponentType == OpponentType.LOCAL_PASS_PLAY) "Player 1" else "$userDisplayName (You)",
            wallsLeft = gameState.leftWalls[0],
            isTurn = gameState.turn == 0 && gameState.winner == null,
            pawnColor = NeonCyan,
            isAi = false,
            modifier = Modifier.fillMaxWidth(0.7f)
        )

        // Action Button under own name view (Resign / Undo / Play Again)
        if (gameState.winner == null && (opponentType == OpponentType.ONLINE || gameState.isAiMatch)) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showResignConfirmation = true },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NeonMagenta),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonMagenta),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(44.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Resign", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        } else if (opponentType == OpponentType.LOCAL_PASS_PLAY && gameState.winner == null) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onUndoMove,
                enabled = gameState.moveHistory.isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (gameState.moveHistory.isNotEmpty()) NeonCyan else NeonBorder),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(44.dp)
            ) {
                Icon(imageVector = Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Undo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        } else if (gameState.winner != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(44.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play Again", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }
    }

    // Confirmation Dialogs
    if (showResignConfirmation) {
        AlertDialog(
            onDismissRequest = { showResignConfirmation = false },
            containerColor = NeonDarkCard,
            title = { Text("Resign Match?", color = Color.White) },
            text = { Text("Are you sure you want to surrender this battle? You will lose trophies.", color = Color(0xFFA0ACCC)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResignConfirmation = false
                        onResign()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta)
                ) {
                    Text("Yes, Resign", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showResignConfirmation = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            containerColor = NeonDarkCard,
            title = { Text("Exit Game?", color = Color.White) },
            text = { Text("The match is still in progress. Exiting now will count as a loss.", color = Color(0xFFA0ACCC)) },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmation = false
                        onResign()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta)
                ) {
                    Text("Exit & Concede", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showExitConfirmation = false }) {
                    Text("Stay", color = Color.White)
                }
            }
        )
    }

    // Online Multiplayer Searching/Error modals (Already exist below)
    // Online Multiplayer Searching Dialog Modal
    if (opponentType == OpponentType.ONLINE && (onlineMatchState == OnlineMatchState.CONNECTING || onlineMatchState == OnlineMatchState.SEARCHING_MATCH)) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = NeonDarkCard,
            titleContentColor = Color.White,
            textContentColor = Color(0xFFA0ACCC),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.1f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = NeonCyan,
                            strokeWidth = 3.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ESTABLISHING UPLINK",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan,
                        letterSpacing = 2.sp
                    )
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (onlineMatchState == OnlineMatchState.CONNECTING) "Syncing with Battle Grid..." else "Scanning for Elite Duelists...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You are currently in the competitive queue. Finding the best match for your skill level...",
                        color = Color(0xFFA0ACCC),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        onCancelOnlineMatchmaking()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, NeonBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Abort Matchmaking", color = Color.White)
                }
            }
        )
    }

    // Online Multiplayer Error Dialog Modal
    if (opponentType == OpponentType.ONLINE && (onlineMatchState == OnlineMatchState.ERROR || onlineErrorMessage != null)) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = NeonDarkCard,
            titleContentColor = NeonMagenta,
            textContentColor = Color(0xFFA0ACCC),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(NeonMagenta.copy(alpha = 0.1f))
                            .border(1.dp, NeonMagenta.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = NeonMagenta,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "UPLINK FAILURE",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonMagenta,
                        letterSpacing = 2.sp
                    )
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = onlineErrorMessage ?: "Protocol mismatch or server unreachable.",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Verify your connection and ensure the Nakama server is active.",
                        color = Color(0xFFA0ACCC),
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onRetryOnlineConnection,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry Link", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        onCancelOnlineMatchmaking()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, NeonBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Return to Base", color = Color.White)
                }
            }
        )
    }

    // Victory/Defeat Celebration Dialog Modal
    if (gameState.winner != null) {
        val isWinner = gameState.winner == myPlayerIndex
        val titleText = if (opponentType == OpponentType.LOCAL_PASS_PLAY) {
            if (gameState.winner == 0) "🏆 PLAYER 1 WINS!" else "🏆 PLAYER 2 WINS!"
        } else if (isWinner) {
            "🏆 YOU WIN!"
        } else {
            "💀 YOU LOSE!"
        }
        val titleColor = if (opponentType == OpponentType.LOCAL_PASS_PLAY || isWinner) NeonCyan else NeonMagenta

        AlertDialog(
            onDismissRequest = {},
            containerColor = NeonDarkCard,
            titleContentColor = titleColor,
            textContentColor = Color(0xFFA0ACCC),
            title = {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = titleColor
                )
            },
            text = {
                Column {
                    Text(if (isWinner) "Masterful strategy! You breached the board defense." else "Your opponent outmaneuvered you this time.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Total Moves: ${gameState.moveHistory.size}", fontWeight = FontWeight.SemiBold, color = NeonCyan)
                    Text("Walls Placed: ${gameState.walls.size}", fontWeight = FontWeight.SemiBold, color = NeonMagenta)
                }
            },
            confirmButton = {
                if (opponentType != OpponentType.ONLINE) {
                    Button(
                        onClick = {
                            onTriggerMatchEndInterstitial {
                                onRestart()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Play Again", fontWeight = FontWeight.ExtraBold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        onTriggerMatchEndInterstitial {
                            onBack()
                        }
                    },
                    border = BorderStroke(1.dp, NeonBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Main Menu", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun WallItemButton(
    isHorizontal: Boolean,
    isSelected: Boolean,
    isEnabled: Boolean,
    selectedColor: Color,
    onSelect: () -> Unit,
    onStartDrag: (Offset) -> Unit,
    onUpdateDrag: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    modifier: Modifier = Modifier,
    isRotated: Boolean = false
) {
    var bounds by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = modifier
            .size(56.dp, 46.dp)
            .onGloballyPositioned { bounds = it.boundsInWindow() }
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput
                val touchSlop = 10.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val b = bounds ?: return@awaitEachGesture
                    val startTouchPos = down.position
                    var hasDragged = false

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            val dist = hypot(change.position.x - startTouchPos.x, change.position.y - startTouchPos.y)
                            if (!hasDragged && dist > touchSlop) {
                                hasDragged = true
                                val startPos = if (isRotated) {
                                    Offset(b.right - change.position.x, b.bottom - change.position.y)
                                } else {
                                    b.topLeft + change.position
                                }
                                onStartDrag(startPos)
                            }

                            if (hasDragged) {
                                val updatePos = if (isRotated) {
                                    Offset(b.right - change.position.x, b.bottom - change.position.y)
                                } else {
                                    b.topLeft + change.position
                                }
                                onUpdateDrag(updatePos)
                                change.consume()
                            }
                        } else {
                            if (hasDragged) {
                                onEndDrag()
                            } else {
                                onSelect()
                            }
                            break
                        }
                    } while (true)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cornerRadiusPx = 12.dp.toPx()

            if (!isEnabled) {
                // Disabled State: Subtle transparent container
                drawRoundRect(
                    color = Color(0xFF111827).copy(alpha = 0.5f),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
                drawRoundRect(
                    color = Color(0xFF1F2937).copy(alpha = 0.5f),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    style = Stroke(width = 1.dp.toPx())
                )
            } else if (isSelected) {
                // Selected State: Clean highlighted container in player's color
                drawRoundRect(
                    color = selectedColor.copy(alpha = 0.18f),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
                drawRoundRect(
                    color = selectedColor,
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            } else {
                // Unselected Enabled State: Clean dark item
                drawRoundRect(
                    color = Color(0xFF111827),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
                drawRoundRect(
                    color = Color(0xFF374151),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Draw Wall Bar Icon
            val centerX = width / 2f
            val centerY = height / 2f

            val wallColor = when {
                !isEnabled -> Color(0xFF475569)
                isSelected -> selectedColor
                else -> Color(0xFFCBD5E1)
            }

            if (isHorizontal) {
                val barW = 26.dp.toPx()
                val barH = 8.dp.toPx()
                val left = centerX - (barW / 2f)
                val top = centerY - (barH / 2f)
                val barRadius = barH / 2f

                drawRoundRect(
                    color = wallColor,
                    topLeft = Offset(left, top),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barRadius, barRadius)
                )
                if (isEnabled) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = if (isSelected) 0.45f else 0.25f),
                        topLeft = Offset(left + 2f, top + 1.5f),
                        size = Size(barW - 4f, 1.5f),
                        cornerRadius = CornerRadius(0.75f, 0.75f)
                    )
                }
            } else {
                val barW = 8.dp.toPx()
                val barH = 26.dp.toPx()
                val left = centerX - (barW / 2f)
                val top = centerY - (barH / 2f)
                val barRadius = barW / 2f

                drawRoundRect(
                    color = wallColor,
                    topLeft = Offset(left, top),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barRadius, barRadius)
                )
                if (isEnabled) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = if (isSelected) 0.45f else 0.25f),
                        topLeft = Offset(left + 1.5f, top + 2f),
                        size = Size(1.5f, barH - 4f),
                        cornerRadius = CornerRadius(0.75f, 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerScoreCard(
    playerName: String,
    wallsLeft: Int,
    isTurn: Boolean,
    pawnColor: Color,
    isAi: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = NeonDarkCard
        ),
        border = BorderStroke(
            width = if (isTurn) 2.dp else 1.dp,
            color = if (isTurn) pawnColor else NeonBorder
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(NeonDarkSurface)
                    .border(
                        width = 1.dp,
                        color = if (isTurn) pawnColor else NeonBorder,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAi) Icons.Default.SmartToy else Icons.Default.Person,
                    contentDescription = "User Avatar",
                    tint = if (isTurn) pawnColor else Color(0xFFA0ACCC),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "Walls: $wallsLeft",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA0ACCC)
                )
            }
            if (isTurn) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(pawnColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "TURN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

