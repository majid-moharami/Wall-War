package com.wallwar.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import com.wallwar.R
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import com.wallwar.data.EmojiSkin
import com.wallwar.ui.theme.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
    isOpponentDisconnected: Boolean = false,
    disconnectSecondsRemaining: Int = 60,
    isLocalDisconnected: Boolean = false,
    localDisconnectSeconds: Int = 15,
    arenaTitle: String = "Pro Arena",
    winningPrize: Int = 0,
    onlineErrorMessage: String? = null,
    matchResultDelta: com.wallwar.data.MatchResultDelta? = null,
    equippedBallSkinId: String = com.wallwar.data.BallSkinCatalog.DEFAULT_EQUIPPED_BALL_ID,
    opponentBallSkinId: String = com.wallwar.data.BallSkinCatalog.DEFAULT_OPPONENT_BALL_ID,
    equippedWallSkinId: String = com.wallwar.data.WallSkinCatalog.DEFAULT_EQUIPPED_WALL_ID,
    opponentWallSkinId: String = com.wallwar.data.WallSkinCatalog.DEFAULT_OPPONENT_WALL_ID,
    playerEmote: EmojiSkin? = null,
    opponentEmote: EmojiSkin? = null,
    allEmojis: List<EmojiSkin> = emptyList(),
    unlockedEmojiIds: Set<String> = emptySet(),
    onSendEmote: (EmojiSkin) -> Unit = {},
    onNavigateToEmojiShop: () -> Unit = {},
    onRetryOnlineConnection: () -> Unit = {},
    onCancelOnlineMatchmaking: () -> Unit = {},
    onForfeitAndQuitLocalMatch: () -> Unit = {},
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
    var showEmotePicker by remember { mutableStateOf(false) }

    val initialP0Skin = if (myPlayerIndex == 0) equippedBallSkinId else opponentBallSkinId
    val initialP1Skin = if (myPlayerIndex == 1) equippedBallSkinId else opponentBallSkinId
    val (resolvedP0BallSkinId, resolvedP1BallSkinId) = remember(equippedBallSkinId, opponentBallSkinId, myPlayerIndex) {
        com.wallwar.data.BallSkinCatalog.resolveMatchBallSkins(
            p0SkinId = initialP0Skin,
            p1SkinId = initialP1Skin,
            userPlayerIndex = myPlayerIndex
        )
    }

    val initialP0WallSkin = if (myPlayerIndex == 0) equippedWallSkinId else opponentWallSkinId
    val initialP1WallSkin = if (myPlayerIndex == 1) equippedWallSkinId else opponentWallSkinId
    val (resolvedP0WallSkinId, resolvedP1WallSkinId) = remember(equippedWallSkinId, opponentWallSkinId, myPlayerIndex) {
        com.wallwar.data.WallSkinCatalog.resolveMatchWallSkins(
            p0SkinId = initialP0WallSkin,
            p1SkinId = initialP1WallSkin,
            userPlayerIndex = myPlayerIndex
        )
    }

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

    val themePrimary = Color(boardTheme.primaryColor)
    val themeOuterTop = Color(boardTheme.outerBgTop)
    val themeOuterBottom = Color(boardTheme.outerBgBottom)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeOuterTop.copy(alpha = 0.85f),
                        NeonDarkBg,
                        themeOuterBottom.copy(alpha = 0.85f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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

            // Center: Title & Subtitle / Winner Reward
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
                Spacer(modifier = Modifier.height(2.dp))
                if (winningPrize > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1D1407),
                        border = BorderStroke(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    NeonAmber.copy(alpha = 0.5f),
                                    NeonAmber,
                                    NeonAmber.copy(alpha = 0.5f)
                                )
                            )
                        ),
                        shadowElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🏆 Reward: ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFE082)
                            )
                            Text(
                                text = "🪙 $winningPrize",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonAmber
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (opponentType == OpponentType.ONLINE) "ONLINE MULTIPLAYER" else if (gameState.isAiMatch) "VS AI (${gameState.aiDifficulty.displayName})" else "Pass & Play",
                        style = MaterialTheme.typography.bodySmall,
                        color = themePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right: Resign + Sound Toggle Button
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Resign Icon Button
                if (gameState.winner == null && (opponentType == OpponentType.ONLINE || gameState.isAiMatch)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NeonDarkCard)
                            .border(1.dp, NeonMagenta.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { showResignConfirmation = true },
                            modifier = Modifier.testTag("btn_resign")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Resign Match",
                                tint = NeonMagenta
                            )
                        }
                    }
                }

                // Sound Toggle Button
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
                            imageVector = if (soundManager.isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
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
                val p2WallRes = com.wallwar.data.WallSkinCatalog.getWallDrawableRes(
                    resolvedP1WallSkinId,
                    R.drawable.ic_red_wall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WallItemButton(
                        isHorizontal = true,
                        isSelected = isWallMode && gameState.turn == 1 && isWallHorizontal,
                        isEnabled = gameState.turn == 1 && gameState.leftWalls[1] > 0 && gameState.winner == null,
                        wallDrawableRes = p2WallRes,
                        selectedColor = NeonMagenta,
                        isRotated = true,
                        onSelect = { onSelectWallOrientation(true) },
                        onStartDrag = { pos -> handleStartWallDrag(true, pos) },
                        onUpdateDrag = { pos -> handleUpdateWallDrag(true, pos) },
                        onEndDrag = handleEndWallDrag
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    WallItemButton(
                        isHorizontal = false,
                        isSelected = isWallMode && gameState.turn == 1 && !isWallHorizontal,
                        isEnabled = gameState.turn == 1 && gameState.leftWalls[1] > 0 && gameState.winner == null,
                        wallDrawableRes = p2WallRes,
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
                    ballDrawableRes = com.wallwar.data.BallSkinCatalog.getBallDrawableRes(resolvedP1BallSkinId, R.drawable.ic_red_ball),
                    turnSecondsRemaining = if (gameState.turn == 1 && gameState.winner == null && opponentType == OpponentType.ONLINE) turnTimeLeft else null,
                    modifier = Modifier.fillMaxWidth(0.7f)
                )
            }
        } else {
            val opponentBallId = if (myPlayerIndex == 0) resolvedP1BallSkinId else resolvedP0BallSkinId
            val opponentBallRes = com.wallwar.data.BallSkinCatalog.getBallDrawableRes(
                opponentBallId,
                if (opponentIndex == 0) R.drawable.ic_blue_ball else R.drawable.ic_red_ball
            )

            PlayerScoreCard(
                playerName = opponentName,
                wallsLeft = gameState.leftWalls[opponentIndex],
                isTurn = gameState.turn == opponentIndex && gameState.winner == null,
                pawnColor = opponentPawnColor,
                isAi = gameState.isAiMatch && opponentIndex == 1,
                ballDrawableRes = opponentBallRes,
                turnSecondsRemaining = if (gameState.turn == opponentIndex && gameState.winner == null && opponentType == OpponentType.ONLINE) turnTimeLeft else null,
                modifier = Modifier.fillMaxWidth(0.7f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                externalIsValidDrag = isValidDrag,
                player0BallSkinId = resolvedP0BallSkinId,
                player1BallSkinId = resolvedP1BallSkinId,
                player0WallSkinId = resolvedP0WallSkinId,
                player1WallSkinId = resolvedP1WallSkinId
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

        // Row 1: Wall items (Icons with real skin textures) + Emote Button
        val myWallSkinId = if (myPlayerIndex == 0) resolvedP0WallSkinId else resolvedP1WallSkinId
        val myWallRes = com.wallwar.data.WallSkinCatalog.getWallDrawableRes(
            myWallSkinId,
            if (myPlayerIndex == 0) R.drawable.ic_blue_wall else R.drawable.ic_red_wall
        )
        val activeWallRes = if (isQuickPassPlay && gameState.turn == 1) {
            com.wallwar.data.WallSkinCatalog.getWallDrawableRes(resolvedP1WallSkinId, R.drawable.ic_red_wall)
        } else {
            myWallRes
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WallItemButton(
                isHorizontal = true,
                isSelected = p1WallSelected && isWallHorizontal,
                isEnabled = p1WallEnabled,
                wallDrawableRes = activeWallRes,
                selectedColor = if (isQuickPassPlay) NeonCyan else currentTurnColor,
                onSelect = { onSelectWallOrientation(true) },
                onStartDrag = { pos -> handleStartWallDrag(true, pos) },
                onUpdateDrag = { pos -> handleUpdateWallDrag(true, pos) },
                onEndDrag = handleEndWallDrag
            )
            Spacer(modifier = Modifier.width(14.dp))
            WallItemButton(
                isHorizontal = false,
                isSelected = p1WallSelected && !isWallHorizontal,
                isEnabled = p1WallEnabled,
                wallDrawableRes = activeWallRes,
                selectedColor = if (isQuickPassPlay) NeonCyan else currentTurnColor,
                onSelect = { onSelectWallOrientation(false) },
                onStartDrag = { pos -> handleStartWallDrag(false, pos) },
                onUpdateDrag = { pos -> handleUpdateWallDrag(false, pos) },
                onEndDrag = handleEndWallDrag
            )

            // Emote Button (Accessible in Online or AI Matches)
            if (opponentType == OpponentType.ONLINE || gameState.isAiMatch) {
                Spacer(modifier = Modifier.width(14.dp))
                Box(
                    modifier = Modifier
                        .size(58.dp, 50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF131A2A))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.8f), NeonMagenta.copy(alpha = 0.8f))),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { showEmotePicker = true }
                        .testTag("btn_open_emotes"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "😎",
                            fontSize = 18.sp
                        )
                        Text(
                            text = "EMOTE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Local Player Info Card
        val myBallId = if (myPlayerIndex == 0) resolvedP0BallSkinId else resolvedP1BallSkinId
        val myBallRes = com.wallwar.data.BallSkinCatalog.getBallDrawableRes(
            myBallId,
            if (myPlayerIndex == 0) R.drawable.ic_blue_ball else R.drawable.ic_red_ball
        )

        val localPlayerIndex = if (opponentType == OpponentType.ONLINE) myPlayerIndex else 0
        val localPawnColor = if (localPlayerIndex == 0) NeonCyan else NeonMagenta

        PlayerScoreCard(
            playerName = if (opponentType == OpponentType.LOCAL_PASS_PLAY) "Player 1" else "$userDisplayName (You)",
            wallsLeft = gameState.leftWalls[localPlayerIndex],
            isTurn = gameState.turn == localPlayerIndex && gameState.winner == null,
            pawnColor = localPawnColor,
            isAi = false,
            ballDrawableRes = myBallRes,
            turnSecondsRemaining = if (gameState.turn == localPlayerIndex && gameState.winner == null && opponentType == OpponentType.ONLINE) turnTimeLeft else null,
            modifier = Modifier.fillMaxWidth(0.7f)
        )

        // Action Button under own name view (Undo / Play Again)
        if (opponentType == OpponentType.LOCAL_PASS_PLAY && gameState.winner == null) {
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
                Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
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

    // Local Connection Lost Modal
    if (opponentType == OpponentType.ONLINE && isLocalDisconnected && gameState.winner == null) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = NeonDarkCard,
            titleContentColor = NeonAmber,
            textContentColor = Color(0xFFA0ACCC),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(NeonAmber.copy(alpha = 0.15f))
                            .border(1.5.dp, NeonAmber, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = NeonAmber,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "CONNECTION LOST",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonAmber,
                        letterSpacing = 1.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Attempting to reconnect to server...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonDarkSurface)
                            .border(1.dp, NeonAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reconnecting: ${localDisconnectSeconds}s",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Re-establishing connection to match...\nIf restored within 60 seconds, the match will automatically resume!",
                        color = Color(0xFFA0ACCC),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onForfeitAndQuitLocalMatch,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Forfeit & Leave Match", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {}
        )
    }

    // Opponent Disconnected Announcement Overlay Dialog Modal
    if (opponentType == OpponentType.ONLINE && isOpponentDisconnected && gameState.winner == null) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = NeonDarkCard,
            titleContentColor = NeonAmber,
            textContentColor = Color(0xFFA0ACCC),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(NeonAmber.copy(alpha = 0.15f))
                            .border(1.5.dp, NeonAmber, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "OPPONENT DISCONNECTED",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonAmber,
                        letterSpacing = 1.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "$onlineOpponentName lost connection.",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonDarkSurface)
                            .border(1.dp, NeonAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Waiting: ${disconnectSecondsRemaining}s",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "If opponent reconnects within 60 seconds, the match will resume.\nOtherwise, you will automatically win!",
                        color = Color(0xFFA0ACCC),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {},
            dismissButton = {}
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
                    Text(
                        text = if (isWinner) "Masterful strategy! You breached the enemy defense." else "Your opponent outmaneuvered you this time.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (matchResultDelta != null) {
                        val delta = matchResultDelta
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NeonDarkSurface),
                            border = BorderStroke(1.dp, if (delta.didWin) NeonCyan.copy(alpha = 0.5f) else NeonMagenta.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Trophies",
                                        fontSize = 13.sp,
                                        color = Color(0xFFA0ACCC)
                                    )
                                    Text(
                                        text = if (delta.trophyDelta >= 0) "+${delta.trophyDelta} 🏆" else "${delta.trophyDelta} 🏆",
                                        fontWeight = FontWeight.Bold,
                                        color = if (delta.trophyDelta >= 0) com.wallwar.ui.theme.NeonEmerald else NeonMagenta,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "XP Gained",
                                        fontSize = 13.sp,
                                        color = Color(0xFFA0ACCC)
                                    )
                                    Text(
                                        text = "+${delta.xpGained} XP",
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Coins Rewarded",
                                        fontSize = 13.sp,
                                        color = Color(0xFFA0ACCC)
                                    )
                                    Text(
                                        text = "+${delta.totalCoinsGained} 🪙",
                                        fontWeight = FontWeight.Bold,
                                        color = com.wallwar.ui.theme.NeonAmber,
                                        fontSize = 13.sp
                                    )
                                }

                                if (delta.currentWinStreak >= 2) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(com.wallwar.ui.theme.NeonAmber.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🔥 ${delta.currentWinStreak}-WIN STREAK (+${delta.streakBonusCoins} Bonus Coins!)",
                                            color = com.wallwar.ui.theme.NeonAmber,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (delta.leveledUp) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NeonCyan.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "⭐ LEVEL UP! REACHED LEVEL ${delta.newLevel} ⭐",
                                            color = NeonCyan,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Moves: ${gameState.moveHistory.size}", fontSize = 12.sp, color = NeonCyan)
                        Text("Walls Placed: ${gameState.walls.size}", fontSize = 12.sp, color = NeonMagenta)
                    }
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

    // Emote Picker Dialog
    if (showEmotePicker) {
        EmotePickerDialog(
            allEmojis = allEmojis,
            unlockedEmojiIds = unlockedEmojiIds,
            onSendEmote = { emoji ->
                showEmotePicker = false
                onSendEmote(emoji)
            },
            onNavigateToShop = {
                showEmotePicker = false
                onNavigateToEmojiShop()
            },
            onDismiss = { showEmotePicker = false }
        )
    }

    // Dynamic Center Screen Emote Overlay with Flying & Pop Animation
    CenterScreenEmoteOverlay(
        playerEmote = playerEmote,
        opponentEmote = opponentEmote,
        userDisplayName = userDisplayName,
        onlineOpponentName = onlineOpponentName,
        opponentType = opponentType,
        myPlayerIndex = myPlayerIndex,
        isAiMatch = gameState.isAiMatch,
        modifier = Modifier.align(Alignment.Center)
    )
    }
}

@Composable
fun WallItemButton(
    isHorizontal: Boolean,
    isSelected: Boolean,
    isEnabled: Boolean,
    selectedColor: Color,
    @androidx.annotation.DrawableRes wallDrawableRes: Int? = null,
    onSelect: () -> Unit,
    onStartDrag: (Offset) -> Unit,
    onUpdateDrag: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    modifier: Modifier = Modifier,
    isRotated: Boolean = false
) {
    var bounds by remember { mutableStateOf<Rect?>(null) }
    val containerShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .size(58.dp, 50.dp)
            .clip(containerShape)
            .background(
                if (!isEnabled) {
                    Color(0xFF0F172A).copy(alpha = 0.5f)
                } else if (isSelected) {
                    selectedColor.copy(alpha = 0.18f)
                } else {
                    Color(0xFF131A2A)
                }
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (!isEnabled) {
                    Color(0xFF1E293B).copy(alpha = 0.5f)
                } else if (isSelected) {
                    selectedColor
                } else {
                    Color(0xFF2A364F)
                },
                shape = containerShape
            )
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
        val barColor = when {
            !isEnabled -> Color(0xFF475569)
            isSelected -> selectedColor
            else -> Color(0xFF94A3B8)
        }

        if (isHorizontal) {
            Box(
                modifier = Modifier
                    .size(26.dp, 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp, 26.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
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
    @androidx.annotation.DrawableRes ballDrawableRes: Int? = null,
    turnSecondsRemaining: Int? = null,
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
            val ballRes = ballDrawableRes ?: if (pawnColor == NeonCyan) R.drawable.ic_blue_ball else R.drawable.ic_red_ball
            Image(
                painter = painterResource(id = ballRes),
                contentDescription = if (pawnColor == NeonCyan) "Blue Ball" else "Red Ball",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (turnSecondsRemaining != null) {
                        val isUrgent = turnSecondsRemaining < 10
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isUrgent) NeonMagenta.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.45f))
                                .border(
                                    1.dp,
                                    if (isUrgent) NeonMagenta else pawnColor.copy(alpha = 0.6f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Turn Timer",
                                tint = if (isUrgent) NeonMagenta else pawnColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${turnSecondsRemaining}s",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUrgent) NeonMagenta else Color.White
                            )
                        }
                    }

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
}

@Composable
fun CenterScreenEmoteOverlay(
    playerEmote: EmojiSkin?,
    opponentEmote: EmojiSkin?,
    userDisplayName: String,
    onlineOpponentName: String,
    opponentType: OpponentType,
    myPlayerIndex: Int,
    isAiMatch: Boolean,
    modifier: Modifier = Modifier
) {
    // Show either opponent emote or player emote
    val activeEmote = opponentEmote ?: playerEmote
    val isOpponent = opponentEmote != null

    val senderName = if (isOpponent) {
        if (opponentType == OpponentType.ONLINE) onlineOpponentName else if (isAiMatch) "AI Bot" else "Player 2"
    } else {
        if (opponentType == OpponentType.LOCAL_PASS_PLAY) "Player 1" else userDisplayName
    }

    val accentColor = if (isOpponent) {
        if (opponentType == OpponentType.ONLINE && myPlayerIndex == 1) NeonCyan else NeonMagenta
    } else {
        NeonCyan
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = activeEmote != null,
        enter = slideInVertically(
            initialOffsetY = { if (isOpponent) -it * 2 else it * 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + scaleIn(
            initialScale = 0.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(animationSpec = tween(200)),
        exit = scaleOut(
            targetScale = 1.4f,
            animationSpec = tween(300, easing = FastOutLinearInEasing)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        activeEmote?.let { emote ->
            val infiniteTransition = rememberInfiniteTransition(label = "center_emote_pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            val rotation by infiniteTransition.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "rotation"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    }
                    .testTag(if (isOpponent) "center_opponent_emote" else "center_player_emote")
            ) {
                // Large Glowing Disc for Emoji
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.35f),
                                    NeonDarkCard.copy(alpha = 0.95f),
                                    Color.Black.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                listOf(accentColor, NeonAmber, accentColor, NeonMagenta, accentColor)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emote.symbol,
                        fontSize = 62.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Emote Name and Sender Tag Banner
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NeonDarkCard.copy(alpha = 0.95f),
                    border = BorderStroke(1.5.dp, accentColor),
                    shadowElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isOpponent) "$senderName:" else "You:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = accentColor
                        )
                        Text(
                            text = emote.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmotePickerDialog(
    allEmojis: List<EmojiSkin>,
    unlockedEmojiIds: Set<String>,
    onSendEmote: (EmojiSkin) -> Unit,
    onNavigateToShop: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeonDarkCard,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "😎 SEND EMOTE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Taunt or react to your opponent (3s)",
                        fontSize = 11.sp,
                        color = NeonCyan
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFA0ACCC)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    items(allEmojis) { emoji ->
                        val isUnlocked = unlockedEmojiIds.contains(emoji.id)

                        Card(
                            onClick = {
                                if (isUnlocked) {
                                    onSendEmote(emoji)
                                } else {
                                    onNavigateToShop()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUnlocked) NeonDarkSurface else Color(0xFF141926)
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (isUnlocked) NeonCyan else Color(0xFF2A334A)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .testTag("emote_item_${emoji.id}")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Text(
                                        text = emoji.symbol,
                                        fontSize = 28.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = emoji.name,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnlocked) Color.White else Color(0xFFA0ACCC),
                                        maxLines = 1
                                    )
                                    if (!isUnlocked) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Locked",
                                                tint = NeonAmber,
                                                modifier = Modifier.size(9.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "${emoji.coinPrice}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = NeonAmber
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onNavigateToShop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_open_emoji_shop_from_dialog")
            ) {
                Text(
                    text = "🛒 Open Emoji Skins Shop",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {}
    )
}

